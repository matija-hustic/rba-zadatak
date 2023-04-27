package com.rba.zadatak.model;

import jakarta.persistence.*;

@Entity
@Table
public class CardHolderExport {

	@Id
	@GeneratedValue
	private long id;

	@ManyToOne
	@JoinColumn
	private CardHolder cardHolder;

	@Column
	private String oib;

	@Column
	private String timestamp;

	@Column
	private String filename;

	@Column
	private Boolean active;

	public CardHolderExport() {

	}

	public CardHolderExport(CardHolder cardHolder, String oib, String timestamp, String filename, boolean active) {
		this.cardHolder = cardHolder;
		this.oib = oib;
		this.timestamp = timestamp;
		this.filename = filename;
		this.active = active;
	}

	public long getId() {
		return id;
	}

	public CardHolder getCardHolder() {
		return cardHolder;
	}

	public void setCardHolder(CardHolder cardHolder) {
		this.cardHolder = cardHolder;
	}

	public String getOib() {
		return oib;
	}

	public void setOib(String oib) {
		this.oib = oib;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	@Override
	public String toString() {
		return "CardHolderExport [id=" + id + ", cardHolderId=" + cardHolder.getId() + ", oib=" + oib + ", timestamp=" + timestamp + ", filename=" + filename + ", active=" + active + "]";
	}
}
