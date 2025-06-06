package com.google.cloud.dns.spi.v1;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.dns.Dns;
import com.google.api.services.dns.model.Change;
import com.google.api.services.dns.model.ChangesListResponse;
import com.google.api.services.dns.model.ManagedZone;
import com.google.api.services.dns.model.ManagedZonesListResponse;
import com.google.api.services.dns.model.Project;
import com.google.api.services.dns.model.ResourceRecordSet;
import com.google.api.services.dns.model.ResourceRecordSetsListResponse;
import com.google.cloud.dns.DnsException;
import com.google.cloud.dns.DnsOptions;
import com.google.cloud.http.HttpTransportOptions;
import java.io.IOException;
import java.util.Map;

public class HttpDnsRpc implements DnsRpc {

  private static final String SORT_BY = "changeSequence";
  private final Dns dns;
  private final DnsOptions options;

  private class DefaultRpcBatch implements RpcBatch {

    private final BatchRequest batch;

    private DefaultRpcBatch(BatchRequest batch) {
      this.batch = batch;
    }

    @Override
    public void addListZones(
        RpcBatch.Callback<ManagedZonesListResponse> callback, Map<Option, ?> options) {
      try {
        listZonesCall(options).queue(batch, toJsonCallback(callback));
      } catch (IOException ex) {
        throw translate(ex, false);
      }
    }

    @Override
    public void addCreateZone(
        ManagedZone zone, RpcBatch.Callback<ManagedZone> callback, Map<Option, ?> options) {
      try {
        createZoneCall(zone, options).queue(batch, toJsonCallback(callback));
      } catch (IOException ex) {
        throw translate(ex, false);
      }
    }

    @Override
    public void addGetZone(
        String zoneName, RpcBatch.Callback<ManagedZone> callback, Map<Option, ?> options) {
      try {
        getZoneCall(zoneName, options).queue(batch, toJsonCallback(callback));
      } catch (IOException ex) {
        throw translate(ex, false);
      }
    }

    @Override
    public void addDeleteZone(String zoneName, RpcBatch.Callback<Void> callback) {
      try {
        deleteZoneCall(zoneName).queue(batch, toJsonCallback(callback));
      } catch (IOException ex) {
        throw translate(ex, false);
      }
    }

    @Override
    public void addGetProject(RpcBatch.Callback<Project> callback, Map<Option, ?> options) {
      try {
        getProjectCall(options).queue(batch, toJsonCallback(callback));
      } catch (IOException ex) {
        throw translate(ex, false);
      }
    }

    @Override
    public void addListRecordSets(
        String zoneName,
        RpcBatch.Callback<ResourceRecordSetsListResponse> callback,
        Map<Option, ?> options) {
      try {
        listRecordSetsCall(zoneName, options).queue(batch, toJsonCallback(callback));
      } catch (IOException ex) {
        throw translate(ex, false);
      }
    }

    @Override
    public void addListChangeRequests(
        String zoneName,
        RpcBatch.Callback<ChangesListResponse> callback,
        Map<Option, ?> options) {
      try {
        listChangeRequestsCall(zoneName, options).queue(batch, toJsonCallback(callback));
      } catch (IOException ex) {
        throw translate(ex, false);
      }
    }

    @Override
    public void addGetChangeRequest(
        String zoneName,
        String changeRequestId,
        RpcBatch.Callback<Change> callback,
        Map<Option, ?> options) {
      try {
        getChangeRequestCall(zoneName, changeRequestId, options)
            .queue(batch, toJsonCallback(callback));
      } catch (IOException ex) {
        throw translate(ex, false);
      }
    }

    @Override
    public void addApplyChangeRequest(
        String zoneName,
        Change changeRequest,
        RpcBatch.Callback<Change> callback,
        Map<Option, ?> options) {
      try {
        applyChangeRequestCall(zoneName, changeRequest, options)
            .queue(batch, toJsonCallback(callback));
      } catch (IOException ex) {
        throw translate(ex, false);
      }
    }

    @Override
    public void submit() {
      try {
        batch.execute();
      } catch (IOException ex) {
        throw translate(ex, false);
      }
    }
  }

  private static <T> JsonBatchCallback<T> toJsonCallback(final RpcBatch.Callback<T> callback) {
    return new JsonBatchCallback<T>() {
      @Override
      public void onSuccess(T response, HttpHeaders httpHeaders) throws IOException {
        callback.onSuccess(response);
      }

      @Override
      public void onFailure(GoogleJsonError googleJsonError, HttpHeaders httpHeaders)
          throws IOException {
        callback.onFailure(googleJsonError);
      }
    };
  }

  private static DnsException translate(IOException ex, boolean idempotent) {
    return new DnsException(ex, idempotent);
  }

  public HttpDnsRpc(DnsOptions options) {
    HttpTransportOptions transportOptions = (HttpTransportOptions) options.getTransportOptions();
    HttpTransport transport = transportOptions.getHttpTransportFactory().create();
    HttpRequestInitializer initializer = transportOptions.getHttpRequestInitializer(options);
    this.dns =
        new Dns.Builder(transport, new JacksonFactory(), initializer)
            .setRootUrl(options.getHost())
            .setApplicationName(options.getApplicationName())
            .build();
    this.options = options;
  }

  @Override
  public ManagedZone create(ManagedZone zone, Map<Option, ?> options) throws DnsException {
    try {
      return createZoneCall(zone, options).execute();
    } catch (IOException ex) {
      throw translate(ex, false);
    }
  }

  private Dns.ManagedZones.Create createZoneCall(ManagedZone zone, Map<Option, ?> options)
      throws IOException {
    return dns.managedZones()
        .create(this.options.getProjectId(), zone)
        .setFields(Option.FIELDS.getString(options));
  }

  @Override
  public ManagedZone getZone(String zoneName, Map<Option, ?> options) throws DnsException {
    try {
      return getZoneCall(zoneName, options).execute();
    } catch (IOException ex) {
      DnsException serviceException = translate(ex, true);
      if (serviceException.getCode() == HTTP_NOT_FOUND) {
        return null;
      }
      throw serviceException;
    }
  }

  private Dns.ManagedZones.Get getZoneCall(String zoneName, Map<Option, ?> options)
      throws IOException {
    return dns.managedZones()
        .get(this.options.getProjectId(), zoneName)
        .setFields(Option.FIELDS.getString(options));
  }

  @Override
  public ListResult<ManagedZone> listZones(Map<Option, ?> options) throws DnsException {
    try {
      ManagedZonesListResponse response = listZonesCall(options).execute();
      return ListResult.of(response.getNextPageToken(), response.getManagedZones());
    } catch (IOException ex) {
      throw translate(ex, true);
    }
  }

  private Dns.ManagedZones.List listZonesCall(Map<Option, ?> options) throws IOException {
    return dns.managedZones()
        .list(this.options.getProjectId())
        .setFields(Option.FIELDS.getString(options))
        .setMaxResults(Option.PAGE_SIZE.getInt(options))
        .setPageToken(Option.PAGE_TOKEN.getString(options));
  }

  @Override
  public boolean deleteZone(String zoneName) throws DnsException {
    try {
      deleteZoneCall(zoneName).execute();
      return true;
    } catch (IOException ex) {
      DnsException serviceException = translate(ex, false);
      if (serviceException.getCode() == HTTP_NOT_FOUND) {
        return false;
      }
      throw serviceException;
    }
  }

  private Dns.ManagedZones.Delete deleteZoneCall(String zoneName) throws IOException {
    return dns.managedZones()
        .delete(this.options.getProjectId(), zoneName);
  }

  @Override
  public Project getProject(Map<Option, ?> options) throws DnsException {
    try {
      return getProjectCall(options).execute();
    } catch (IOException ex) {
      throw translate(ex, true);
    }
  }

  private Dns.Projects.Get getProjectCall(Map<Option, ?> options) throws IOException {
    return dns.projects()
        .get(this.options.getProjectId())
        .setFields(Option.FIELDS.getString(options));
  }

  @Override
  public ListResult<ResourceRecordSet> listRecordSets(String zoneName, Map<Option, ?> options)
      throws DnsException {
    try {
      ResourceRecordSetsListResponse response = listRecordSetsCall(zoneName, options).execute();
      return ListResult.of(response.getNextPageToken(), response.getRrsets());
    } catch (IOException ex) {
      throw translate(ex, true);
    }
  }

  private Dns.ResourceRecordSets.List listRecordSetsCall(String zoneName, Map<Option, ?> options)
      throws IOException {
    return dns.resourceRecordSets()
        .list(this.options.getProjectId(), zoneName)
        .setFields(Option.FIELDS.getString(options))
        .setMaxResults(Option.PAGE_SIZE.getInt(options))
        .setPageToken(Option.PAGE_TOKEN.getString(options));
  }

  @Override
  public ListResult<Change> listChangeRequests(String zoneName, Map<Option, ?> options)
      throws DnsException {
    try {
      ChangesListResponse response = listChangeRequestsCall(zoneName, options).execute();
      return ListResult.of(response.getNextPageToken(), response.getChanges());
    } catch (IOException ex) {
      throw translate(ex, true);
    }
  }

  private Dns.Changes.List listChangeRequestsCall(String zoneName, Map<Option, ?> options)
      throws IOException {
    return dns.changes()
        .list(this.options.getProjectId(), zoneName)
        .setFields(Option.FIELDS.getString(options))
        .setMaxResults(Option.PAGE_SIZE.getInt(options))
        .setPageToken(Option.PAGE_TOKEN.getString(options));
  }

  @Override
  public Change applyChangeRequest(
      String zoneName, Change changeRequest, Map<Option, ?> options) throws DnsException {
    try {
      return applyChangeRequestCall(zoneName, changeRequest, options).execute();
    } catch (IOException ex) {
      throw translate(ex, false);
    }
  }

  private Dns.Changes.Create applyChangeRequestCall(
      String zoneName, Change changeRequest, Map<Option, ?> options) throws IOException {
    return dns.changes()
        .create(this.options.getProjectId(), zoneName, changeRequest)
        .setFields(Option.FIELDS.getString(options));
  }

  @Override
  public RpcBatch createBatch() {
    return new DefaultRpcBatch(dns.batch());
  }
}