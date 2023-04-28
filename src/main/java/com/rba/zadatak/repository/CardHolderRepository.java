package com.rba.zadatak.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rba.zadatak.model.CardHolder;

public interface CardHolderRepository extends JpaRepository<CardHolder, Long> {

	Optional<CardHolder> findByOib(String oib);
}
