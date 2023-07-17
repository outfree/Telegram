package ru.sms_activate.response.api_rent.extra;

import org.jetbrains.annotations.NotNull;

public class SMSActivateSMS {
  /**
   * Phone where the sms came from.
   */
  private String phoneFrom;

  /**
   * Text sms.
   */
  private String text;

  /**
   * SMS arrival date.
   */
  private String date;

  private SMSActivateSMS() {
  }

  /**
   * Returns the phone from such a sms came.
   *
   * @return phone from such a sms came.
   */
  public String getPhoneFrom() {
    return phoneFrom;
  }

  /**
   * Returns the text sms.
   *
   * @return text sms.
   */
  @NotNull
  public String getText() {
    return text;
  }

  /**
   * Returns the sms arrival date.
   *
   * @return sms arrival date.
   */
  @NotNull
  public String getDate() {
    return date;
  }

  @Override
  public String toString() {
    return "SMSActivateSMS{" +
      "phoneFrom=" + phoneFrom +
      ", text='" + text + '\'' +
      ", date='" + date + '\'' +
      '}';
  }
}
