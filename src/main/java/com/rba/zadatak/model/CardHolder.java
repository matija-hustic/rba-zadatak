package com.rba.zadatak.model;

import jakarta.persistence.*;

@Entity
public class CardHolder {

  @Id
  @GeneratedValue
  public Long id;

  @Column
  public String firstName;

  @Column
  public String lastName;

  @Column
  public String oib;

  @Column
  public String cardStatus;

  @Column
  public Boolean active;
}
