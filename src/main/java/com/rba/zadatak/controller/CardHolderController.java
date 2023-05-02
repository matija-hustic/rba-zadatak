package com.rba.zadatak.controller;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.GsonBuilder;
import com.rba.zadatak.model.CardHolder;
import com.rba.zadatak.model.CardHolderExport;
import com.rba.zadatak.model.CardHolderFile;
import com.rba.zadatak.model.CardStatus;
import com.rba.zadatak.repository.CardHolderExportRepository;
import com.rba.zadatak.repository.CardHolderRepository;

@RestController
public class CardHolderController {

  @Autowired
  CardHolderRepository cardHolderRepository;

  @Autowired
  CardHolderExportRepository cardHolderExportRepository;

  @PostMapping("/card-holders")
  public ResponseEntity<CardHolder> createCardHolder(@RequestBody CardHolder newCardHolder) {
    // Card holder active by default
    if (newCardHolder.active == null) {
      newCardHolder.active = true;
    }

    // Validate card status
    String validatedCardStatus = newCardHolder.cardStatus == null ? CardStatus.Status1.name() : Stream.of(CardStatus.values())
      .map((cardStatusValue) -> cardStatusValue.name())
      .filter((cardStatusName) -> cardStatusName.equalsIgnoreCase(newCardHolder.cardStatus))
      .findAny().orElse(null);
    if (validatedCardStatus == null) {
      return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
    }
    newCardHolder.cardStatus = validatedCardStatus;

    // Validate card holder oib
    if (newCardHolder.oib == null || !newCardHolder.oib.matches("^\\d{11}$")) {
      return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // Card holder may not exist
    Optional<CardHolder> oldCardHolder = cardHolderRepository.findByOib(newCardHolder.oib);
    if (oldCardHolder.isPresent()) {
      return new ResponseEntity<>(HttpStatus.CONFLICT);
    }

    CardHolder cardHolder = cardHolderRepository.save(newCardHolder);

    cardHolder.id = null;
    return new ResponseEntity<>(cardHolder, HttpStatus.CREATED);
  }

  @PutMapping("/card-holders/{oib}")
  public ResponseEntity<CardHolder> updateCardHolder(@PathVariable String oib, @RequestBody CardHolder newCardHolder) {
    // Card holder active by default
    if (newCardHolder.active == null) {
      newCardHolder.active = true;
    }

    // Validate card status
    String validatedCardStatus = newCardHolder.cardStatus == null ? CardStatus.Status1.name() : Stream.of(CardStatus.values())
      .map((cardStatusValue) -> cardStatusValue.name())
      .filter((cardStatusName) -> cardStatusName.equalsIgnoreCase(newCardHolder.cardStatus))
      .findAny().orElse(null);
    if (validatedCardStatus == null) {
      return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
    }
    newCardHolder.cardStatus = validatedCardStatus;

    // Validate card holder oib
    if (!oib.equals(newCardHolder.oib)) {
      return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // Card holder must exist
    Optional<CardHolder> oldCardHolder = cardHolderRepository.findByOib(oib);
    if (oldCardHolder.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // Use existing card holder id
    newCardHolder.id = oldCardHolder.get().id;

    CardHolder cardHolder = cardHolderRepository.save(newCardHolder);

    cardHolder.id = null;
    return new ResponseEntity<>(cardHolder, HttpStatus.OK);
  }

  @DeleteMapping("/card-holders/{oib}")
  public ResponseEntity<HttpStatus> deleteCardHolder(@PathVariable String oib) {
    // Card holder must exist
    Optional<CardHolder> cardHolder = cardHolderRepository.findByOib(oib);
    if (cardHolder.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // When removing a card holder's data, all exports must be set as inactive and
    // relation to the card holder removed
    List<CardHolderExport> cardHolderExports =
      cardHolderExportRepository.findByCardHolder(cardHolder.get());
    cardHolderExports.forEach((cardHolderExport) -> cardHolderExport.cardHolder = null);;
    cardHolderExports.forEach((cardHolderExport) -> cardHolderExport.active = false);;
    cardHolderExportRepository.saveAll(cardHolderExports);

    cardHolderRepository.deleteById(cardHolder.get().id);

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @GetMapping("/card-holders/{oib}")
  public ResponseEntity<CardHolder> getCardHolder(@PathVariable String oib) {
    // Card holder must exist
    Optional<CardHolder> cardHolder = cardHolderRepository.findByOib(oib);
    if (cardHolder.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    cardHolder.get().id = null;
    return new ResponseEntity<>(cardHolder.get(), HttpStatus.OK);
  }

  @PostMapping("/card-holders/{oib}/exports")
  public ResponseEntity<CardHolderExport> createCardHolderExport(@PathVariable String oib) {
    // Card holder must exist
    Optional<CardHolder> cardHolder = cardHolderRepository.findByOib(oib);
    if (cardHolder.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // A card holder may have only one active export
    Optional<CardHolderExport> activeCardHolderExport = cardHolderExportRepository
      .findByCardHolderAndActiveTrue(cardHolder.get());
    if (activeCardHolderExport.isPresent()) {
      activeCardHolderExport.get().active = false;
      cardHolderExportRepository.save(activeCardHolderExport.get());
    }

    String timestamp = String.valueOf(System.currentTimeMillis());
    String filename = oib + "_" + timestamp + ".json";
    String directory = "exports";

    // Prepare contents for card holder export table
    CardHolderExport cardHolderExport = new CardHolderExport();
    cardHolderExport.cardHolder = cardHolder.get();
    cardHolderExport.oib = oib;
    cardHolderExport.timestamp = timestamp;
    cardHolderExport.filename = filename;
    cardHolderExport.active = true;

    cardHolderExportRepository.save(cardHolderExport);

    // Prepare card holder data for export
    CardHolderFile cardHolderFile = new CardHolderFile();
    cardHolderFile.firstName = cardHolder.get().firstName;
    cardHolderFile.lastName = cardHolder.get().lastName;
    cardHolderFile.oib = cardHolder.get().oib;
    cardHolderFile.cardStatus = cardHolder.get().cardStatus;
    String fileContents = new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(cardHolderFile);

    // Write card holder data to file
    new File(directory).mkdirs();
    try {
      FileWriter myWriter = new FileWriter(new File(directory, filename));
      myWriter.write(fileContents);
      myWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    cardHolderExport.id = null;
    cardHolderExport.cardHolder.id = null;
    return new ResponseEntity<>(cardHolderExport, HttpStatus.CREATED);
  }

  @GetMapping("/card-holders/{oib}/exports")
  public ResponseEntity<List<CardHolderExport>> getCardHolderExports(@PathVariable String oib) {
    // Card holder must exist
    Optional<CardHolder> cardHolder = cardHolderRepository.findByOib(oib);
    if (cardHolder.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    List<CardHolderExport> cardHolderExports =
      cardHolderExportRepository.findByCardHolder(cardHolder.get());

    cardHolderExports.forEach((cardHolderExport) -> cardHolderExport.id = null);;
    cardHolderExports.forEach((cardHolderExport) -> cardHolderExport.cardHolder = null);;
    return new ResponseEntity<>(cardHolderExports, HttpStatus.OK);
  }
}
