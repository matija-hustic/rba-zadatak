package com.rba.zadatak.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rba.zadatak.model.CardHolder;
import com.rba.zadatak.model.CardHolderExport;

public interface CardHolderExportRepository extends JpaRepository<CardHolderExport, Long> {
  
  List<CardHolderExport> findByCardHolder(CardHolder cardHolder);
  Optional<CardHolderExport> findByCardHolderAndActiveTrue(CardHolder cardHolder);
}
