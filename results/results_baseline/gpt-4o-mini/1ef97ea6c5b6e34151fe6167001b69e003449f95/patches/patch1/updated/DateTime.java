package com.github.knaufk.flink.faker;

import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import net.datafaker.DateAndTime;
import net.datafaker.Faker;

public class DateTime extends DateAndTime {

  protected DateTime(Faker faker) {
    super(faker);
  }

  public Timestamp past(int atMost, TimeUnit unit) {
    Date date = super.past(atMost, unit);
    return new Timestamp(date.getTime());
  }

  public Timestamp past(int atMost, int minimum, TimeUnit unit) {
    Date date = super.past(atMost, minimum, unit);
    return new Timestamp(date.getTime());
  }

  public Timestamp future(int atMost, TimeUnit unit) {
    Date date = super.future(atMost, unit);
    return new Timestamp(date.getTime());
  }

  public Timestamp future(int atMost, int minimum, TimeUnit unit) {
    Date date = super.future(atMost, minimum, unit);
    return new Timestamp(date.getTime());
  }

  public Timestamp future(int atMost, TimeUnit unit, Date referenceDate) {
    Date date = super.future(atMost, unit, referenceDate);
    return new Timestamp(date.getTime());
  }

  public Timestamp past(int atMost, TimeUnit unit, Date referenceDate) {
    Date date = super.past(atMost, unit, referenceDate);
    return new Timestamp(date.getTime());
  }

  public Timestamp between(Date from, Date to) throws IllegalArgumentException {
    Date date = super.between(from, to);
    return new Timestamp(date.getTime());
  }

  public Timestamp birthday() {
    Date date = super.birthday();
    return new Timestamp(date.getTime());
  }

  public Timestamp birthday(int minAge, int maxAge) {
    Date date = super.birthday(minAge, maxAge);
    return new Timestamp(date.getTime());
  }
}