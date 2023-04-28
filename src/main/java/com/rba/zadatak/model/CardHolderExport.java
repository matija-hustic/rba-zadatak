package com.rba.zadatak.model;

import jakarta.persistence.*;

@Entity
public class CardHolderExport {

	@Id
	@GeneratedValue
	public Long id;

	@ManyToOne
	public CardHolder cardHolder;

	@Column
	public String oib;

	@Column
	public String timestamp;

	@Column
	public String filename;

	@Column
	public Boolean active;
}
