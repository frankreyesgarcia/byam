package com.pinterest.singer.loggingaudit.client;

import com.pinterest.singer.loggingaudit.client.common.LoggingAuditClientMetrics;
import com.pinterest.singer.loggingaudit.thrift.LoggingAuditHeaders;
import com.pinterest.singer.loggingaudit.thrift.LoggingAuditEvent;
import com.pinterest.singer.loggingaudit.thrift.LoggingAuditStage;
import com.pinterest.singer.loggingaudit.thrift.configuration.KafkaSenderConfig;
import com.pinterest.singer.metrics.OpenTsdbMetricConverter;
import com.pinterest.singer.utils.CommonUtils;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuditEventKafkaSender implements LoggingAuditEventSender {

  private static final Logger LOG = LoggerFactory.getLogger(AuditEventKafkaSender.class);
  private static final int MAX_RETRIES_FOR_SELECTION_RANDOM_PARTITION = 10;
  private static final int PARTITIONS_REFRESH_INTERVAL_IN_SECONDS = 30;
  private static final int NUM_OF_PARTITIONS_TO_TRY_SENDING = 3;
  private static final int DEQUEUE_WAIT_IN_SECONDS = 30;
  private static final int THREAD_SLEEP_IN_SECONDS = 10;
  private int stopGracePeriodInSeconds = 300;
  private final LoggingAuditStage stage;
  private final String host;
  private final LinkedBlockingDeque<LoggingAuditEvent> queue;
  private KafkaProducer<byte[], byte[]> kafkaProducer;
  private TSerializer serializer = new TSerializer();
  private AtomicBoolean cancelled = new AtomicBoolean(false);
  private String topic;
  private String name;
  private Thread thread;
  private List<PartitionInfo> partitionInfoList = new ArrayList<>();
  private long lastTimeUpdate = -1;
  private Set<Integer> badPartitions = ConcurrentHashMap.newKeySet();
  private Map<LoggingAuditHeaders, Integer> eventTriedCount = new ConcurrentHashMap<>();
  private int currentPartitionId = -1;

  public AuditEventKafkaSender(KafkaSenderConfig config,
                               LinkedBlockingDeque<LoggingAuditEvent> queue,
                               LoggingAuditStage stage, String host, String name) {
    this.topic = config.getTopic();
    this.queue = queue;
    this.stage = stage;
    this.host = host;
    this.name = name;
    this.stopGracePeriodInSeconds = config.getStopGracePeriodInSeconds();
    this.badPartitions.add(-1);
  }

  public KafkaProducer<byte[], byte[]> getKafkaProducer() {
    return kafkaProducer;
  }

  public void setKafkaProducer(KafkaProducer<byte[], byte[]> kafkaProducer) {
    this.kafkaProducer = kafkaProducer;
  }

  private void refreshPartitionIfNeeded() {
    if (System.currentTimeMillis() - lastTimeUpdate > 1000 * PARTITIONS_REFRESH_INTERVAL_IN_SECONDS) {
      try {
        badPartitions.clear();
        badPartitions.add(-1);
        partitionInfoList = this.kafkaProducer.partitionsFor(topic);
        lastTimeUpdate = System.currentTimeMillis();
        OpenTsdbMetricConverter.incr(
            LoggingAuditClientMetrics.AUDIT_CLIENT_SENDER_KAFKA_PARTITIONS_REFRESH_COUNT, 1,
                "host=" + host, "stage=" + stage.toString());
      } catch (Exception e) {
        OpenTsdbMetricConverter.incr(
            LoggingAuditClientMetrics.AUDIT_CLIENT_SENDER_KAFKA_PARTITIONS_REFRESH_ERROR, 1,
                "host=" + host, "stage=" + stage.toString());
      }
    }
    resetCurrentPartitionIdIfNeeded();
  }

  private void resetCurrentPartitionIdIfNeeded() {
    if (partitionInfoList.size() == 0) {
      currentPartitionId = -1;
      return;
    }
    if (badPartitions.contains(currentPartitionId)){
      int trial = 0;
      while (trial < MAX_RETRIES_FOR_SELECTION_RANDOM_PARTITION) {
        trial += 1;
        int index = ThreadLocalRandom.current().nextInt(partitionInfoList.size());
        int randomPartition = partitionInfoList.get(index).partition();
        if (!badPartitions.contains(randomPartition)) {
          LOG.warn("Change current partition of audit event topic from {} to {}", currentPartitionId,
              randomPartition);
          currentPartitionId = randomPartition;
          OpenTsdbMetricConverter.incr(
              LoggingAuditClientMetrics.AUDIT_CLIENT_SENDER_KAFKA_CURRENT_PARTITION_RESET, 1,
              "host=" + host, "stage=" + stage.toString());
          return;
        }
      }
      currentPartitionId =  partitionInfoList.get(ThreadLocalRandom.current().nextInt(
          partitionInfoList.size())).partition();
      LOG.warn("After {} trials, set current partition to {}",
          MAX_RETRIES_FOR_SELECTION_RANDOM_PARTITION, currentPartitionId);
    }
  }

  @Override
  public void run() {
    LoggingAuditEvent event = null;
    ProducerRecord<byte[], byte[]> record;
    byte[] value = null;

    while (!cancelled.get()) {
      try {
        refreshPartitionIfNeeded();
        if (currentPartitionId == -1){
          Thread.sleep(100);
          continue;
        }
        event = queue.poll(DEQUEUE_WAIT_IN_SECONDS, TimeUnit.SECONDS);
        if (event != null) {
          try {
            value = serializer.serialize(event);
            record = new ProducerRecord<>(this.topic, currentPartitionId , null, value);
            kafkaProducer.send(record, new KafkaProducerCallback(event, currentPartitionId));
          } catch (TException e) {
            LOG.debug("[{}] failed to construct ProducerRecord because of serialization exception.",
                Thread.currentThread().getName(), e);
            OpenTsdbMetricConverter
                .incr(LoggingAuditClientMetrics.AUDIT_CLIENT_SENDER_SERIALIZATION_EXCEPTION, 1,
                    "host=" + host, "stage=" + stage.toString(),
                    "logName=" + event.getLoggingAuditHeaders().getLogName());
            eventTriedCount.remove(event.getLoggingAuditHeaders());
          }
        }
      } catch (InterruptedException e) {
        LOG.warn("[{}] got interrupted when polling the queue and while loop is ended!",
            Thread.currentThread().getName(), e);
        OpenTsdbMetricConverter.incr(
            LoggingAuditClientMetrics.AUDIT_CLIENT_SENDER_DEQUEUE_INTERRUPTED_EXCEPTION, 1,
                "host=" + host, "stage=" + stage.toString());
        break;
      } catch (Exception e) {
        LOG.warn("Exit the while loop and finish the thread execution due to exception: ", e);
        OpenTsdbMetricConverter.incr(LoggingAuditClientMetrics.AUDIT_CLIENT_SENDER_EXCEPTION, 1,
            "host=" + host, "stage=" + stage.toString());
        break;
      }
    }
  }

  public class KafkaProducerCallback implements Callback {

    private LoggingAuditEvent event;
    private int partition;

    public KafkaProducerCallback(LoggingAuditEvent event, int partition) {
      this.event = event;
      this.partition = partition;
    }

    public void checkAndEnqueueWhenSendFailed() {
      badPartitions.add(this.partition);
      OpenTsdbMetricConverter
          .incr(LoggingAuditClientMetrics.AUDIT_CLIENT_SENDER_KAFKA_PARTITION_ERROR, 1,
              "host=" + host, "stage=" + stage.toString(), "topic=" + topic,
              "partition=" + this.partition);

      Integer count = eventTriedCount.get(event.getLoggingAuditHeaders());
      if (count == null){
        eventTriedCount.put(event.getLoggingAuditHeaders(), 1);
        insertEvent(event);
        OpenTsdbMetricConverter
            .gauge(LoggingAuditClientMetrics.AUDIT_CLIENT_SENDER_KAFKA_EVENTS_RETRIED,
                eventTriedCount.size(), "host=" + host, "stage=" + stage.toString(),
                "topic=" + topic);
      } else if (count >= NUM_OF_PARTITIONS_TO_TRY_SENDING) {
          LOG.debug("Failed to send audit event after trying {} partitions. Drop event.", count);
          OpenTsdbMetricConverter
              .incr(LoggingAuditClientMetrics.AUDIT_CLIENT_SENDER_KAFKA_EVENTS_DROPPED, 1,
                  "host=" + host, "stage=" + stage.toString(),
                  "logName=" + event.getLoggingAuditHeaders().getLogName());
          eventTriedCount.remove(event.getLoggingAuditHeaders());
      } else {
          eventTriedCount.put(event.getLoggingAuditHeaders(), count + 1);
          insertEvent(event);
      }
    }

    public void insertEvent(LoggingAuditEvent event){
      try {
        boolean success = queue.offerFirst(event, 3, TimeUnit.SECONDS);
        if (!success) {
          LOG.debug("Failed to enqueue LoggingAuditEvent at head of the queue when executing "
              + "producer send callback. Drop this event.");
          eventTriedCount.remove(event.getLoggingAuditHeaders());
        }
      } catch (InterruptedException ex) {
        LOG.debug(
            "Enqueuing LoggingAuditEvent at head of the queue was interrupted in callback. "
                + "Drop this event");
        eventTriedCount.remove(event.getLoggingAuditHeaders());
      }
    }

    @Override
    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
      try {
        if (e == null) {
          OpenTsdbMetricConverter
              .incr(LoggingAuditClientMetrics.AUDIT_CLIENT_SENDER_KAFKA_EVENTS_ACKED, 1,
                  "host=" + host, "stage=" + stage.toString(),
                  "logName=" + event.getLoggingAuditHeaders().getLogName());
          eventTriedCount.remove(event.getLoggingAuditHeaders());
          badPartitions.remove(recordMetadata.partition());
        } else {
          checkAndEnqueueWhenSendFailed();
        }
      } catch (Throwable t) {
        LOG.warn("Exception throws in the callback. Drop this event {}", event, t);
        OpenTsdbMetricConverter
            .incr(LoggingAuditClientMetrics.AUDIT_CLIENT_SENDER_KAFKA_CALLBACK_EXCEPTION, 1,
                "host=" + host, "stage=" + stage.toString(), "topic=" + topic);
      }
    }
  }

  public synchronized void start() {
    if (this.thread == null) {
      thread = new Thread(this);
      thread.setDaemon(true);
      thread.setName(name);
      thread.start();
      LOG.warn(
          "[{}] created and started [{}] to let it dequeue LoggingAuditEvents and send to Kafka.",
          Thread.currentThread().getName(), name);
    }
  }

  public synchronized void stop() {
    LOG.warn(
        "[{}] waits up to {} seconds to let [{}] send out LoggingAuditEvents left in the queue if"
            + " any.",
        Thread.currentThread().getName(), stopGracePeriodInSeconds, name);
    int i = 0;
    int numOfRounds = stopGracePeriodInSeconds / THREAD_SLEEP_IN_SECONDS;
    while (queue.size() > 0 && this.thread != null && thread.isAlive() && i < numOfRounds) {
      i += 1;
      try {
        Thread.sleep(THREAD_SLEEP_IN_SECONDS * 1000);
        CommonUtils.reportQueueUsage(queue.size(), queue.remainingCapacity(), host, stage.toString());
        LOG.info("In {} round, [{}] waited {} seconds and the current queue size is {}", i,
            Thread.currentThread().getName(), THREAD_SLEEP_IN_SECONDS, queue.size());
      } catch (InterruptedException e) {
        LOG.warn("[{}] got interrupted while waiting for [{}] to send out LoggingAuditEvents left "
            + "in the queue.", Thread.currentThread().getName(), name, e);
      }
    }
    cancelled.set(true);
    if (this.thread != null && thread.isAlive()) {
      thread.interrupt();
    }
    try {
      this.kafkaProducer.close();
    } catch (Throwable t) {
      LOG.warn("Exception is thrown while stopping {}.", name, t);
    }
    LOG.warn("[{}] is stopped and the number of LoggingAuditEvents left in the queue is {}.", name,
        queue.size());
  }

}