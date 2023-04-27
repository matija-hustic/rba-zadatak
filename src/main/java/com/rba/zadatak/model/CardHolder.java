package com.rba.zadatak.model;

import jakarta.persistence.*;

@Entity
@Table
public class CardHolder {

	@Id
	@GeneratedValue
	private long id;

	@Column
	private String firstName;

	@Column
	private String lastName;

	@Column
	private String oib;

	@Column
	private String cardStatus;

	@Column
	private Boolean active;

	public CardHolder() {

	}

	public CardHolder(String firstName, String lastName, String oib, String cardStatus, boolean active) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.oib = oib;
		this.cardStatus = cardStatus;
		this.active = active;
	}

	public long getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getOib() {
		return oib;
	}

	public void setOib(String oib) {
		this.oib = oib;
	}

	public String getCardStatus() {
		return cardStatus;
	}

	public void setCardStatus(String cardStatus) {
		this.cardStatus = cardStatus;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	@Override
	public String toString() {
		return "CardHolder [id=" + id + ", firstName=" + firstName + ", lastName=" + lastName + ", oib=" + oib + ", cardStatus=" + cardStatus + ", active=" + active + "]";
	}
}
