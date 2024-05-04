package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.repository.BalanceHistoryRepository;

import com.shepherdmoney.interviewproject.vo.request.Foo;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
public class CreditCardController {
    @Autowired
    CreditCardRepository creditCardRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    BalanceHistoryRepository balanceHistoryRepository;

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with
        // user with given userId
        // Return 200 OK with the credit card id if the user exists and credit card is
        // successfully associated with the user
        // Return other appropriate response code for other exception cases
        // Do not worry about validating the card number, assume card number could be
        // any arbitrary format and length

        return userRepository.findById(payload.getUserId())
                .map(user -> {
                    CreditCard creditCard = new CreditCard();
                    creditCard.setIssuanceBank(payload.getCardIssuanceBank());
                    creditCard.setNumber(payload.getCardNumber());
                    creditCard.setUser(user);
                    creditCardRepository.save(creditCard);
                    return new ResponseEntity<>(creditCard.getId(), HttpStatus.OK);
                }).orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId,
        // using CreditCardView class
        // if the user has no credit card, return empty list, never return null
        if (!userRepository.existsById(userId)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        List<CreditCardView> creditCards = new ArrayList<>();
        creditCardRepository.findByUserId(userId).forEach(creditCard -> {
            creditCards.add(new CreditCardView(creditCard.getIssuanceBank(), creditCard.getNumber()));
        });
        return new ResponseEntity<>(creditCards, HttpStatus.OK);
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user
        // associated with the credit card
        // If so, return the user id in a 200 OK response. If no such user exists,
        // return 400 Bad Request

        // System.out.println(creditCardRepository.findByNumber(creditCardNumber));
        Integer userId = creditCardRepository.findFirstByNumber(creditCardNumber)
                .map(creditCard -> creditCard.getUser().getId()).orElse(null);
        if (userId == null || !userRepository.existsById(userId)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(userId, HttpStatus.OK);
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<Integer> postMethodName(@RequestBody UpdateBalancePayload[] payload) {
        // TODO: Given a list of transactions, update credit cards' balance history.
        // 1. For the balance history in the credit card
        // 2. If there are gaps between two balance dates, fill the empty date with the
        // balance of the previous date
        // 3. Given the payload `payload`, calculate the balance different between the
        // payload and the actual balance stored in the database
        // 4. If the different is not 0, update all the following budget with the
        // difference
        // For example: if today is 4/12, a credit card's balanceHistory is
        // [{date:4/12, balance: 110}, {date: 4/10, balance: 100}],
        // Given a balance amount of {date: 4/11, amount: 110}, the new balanceHistory
        // is
        // [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10,balance:
        // 100}]
        // This is because
        // 1. You would first populate 4/11 with previous day's balance (4/10), so
        // {date: 4/11, amount: 100}
        // 2. And then you observe there is a +10 difference
        // 3. You propagate that +10 difference until today
        // Return 200 OK if update is done and successful, 400 Bad Request if the given
        // card number
        // is not associated with a card.

        // payload.map();

        boolean x = true;

        for (UpdateBalancePayload updateBalancePayload : payload) {
            Optional<CreditCard> creditCardData = creditCardRepository
                    .findFirstByNumber(updateBalancePayload.getCreditCardNumber());
            if (!creditCardData.isPresent()) {
                continue;
            }

            CreditCard creditCard = creditCardData.get();
            List<BalanceHistory> balanceHistories = creditCard.getBalanceHistories();

            System.out.println(balanceHistories.size());
            System.out.println(updateBalancePayload.getBalanceDate());

            Integer closestPreviousIndex = getClosestPreviousIndex(balanceHistories,
                    updateBalancePayload.getBalanceDate());

            if (closestPreviousIndex != -1) {
                double difference = updateBalancePayload.getBalanceAmount()
                        - balanceHistories.get(closestPreviousIndex).getBalance();
                for (int i = closestPreviousIndex + 1; i < balanceHistories.size(); i++) {
                    balanceHistories.get(i).setBalance(balanceHistories.get(i).getBalance() + difference);
                }
            }

            if (closestPreviousIndex != -1
                    && balanceHistories.get(closestPreviousIndex).getDate().compareTo(
                            updateBalancePayload.getBalanceDate()) == 0) {
                balanceHistories.get(closestPreviousIndex).setBalance(updateBalancePayload.getBalanceAmount());
                balanceHistoryRepository.saveAndFlush(balanceHistories.get(closestPreviousIndex));
            } else {
                BalanceHistory newBalanceHistory = new BalanceHistory();
                newBalanceHistory.setDate(updateBalancePayload.getBalanceDate());
                newBalanceHistory.setBalance(updateBalancePayload.getBalanceAmount());
                newBalanceHistory.setCreditCard(creditCard);
                balanceHistories.add(closestPreviousIndex + 1, newBalanceHistory);
                balanceHistoryRepository.saveAndFlush(newBalanceHistory);
            }
            fillUntilToday(balanceHistories, creditCard);
        }

        Optional<CreditCard> creditCardData = creditCardRepository
                .findFirstByNumber("8888");
        if (creditCardData.isPresent()) {
            CreditCard creditCard = creditCardData.get();
            for (BalanceHistory balanceHistory : creditCard.getBalanceHistories()) {
                System.out.println(String.format("%s %s", balanceHistory.getDate(),
                        balanceHistory.getBalance()));
            }
        }
        return new ResponseEntity<>(payload.length, HttpStatus.OK);
    }

    private void fillUntilToday(List<BalanceHistory> balanceHistories, CreditCard creditCard) {
        Integer i = 0;
        LocalDate date = balanceHistories.get(i).getDate();
        Double balance = balanceHistories.get(i).getBalance();

        while (LocalDate.of(2024, 5, 31)
                .compareTo(date) > 0) {
            date = date.plusDays(1);

            if (i < balanceHistories.size() - 1 && date.compareTo(balanceHistories.get(i + 1).getDate()) == 0) {
                i++;
                date = balanceHistories.get(i).getDate();
                balance = balanceHistories.get(i).getBalance();
            } else {
                BalanceHistory newBalanceHistory = new BalanceHistory();
                newBalanceHistory.setDate(date);
                newBalanceHistory.setBalance(balance);
                newBalanceHistory.setCreditCard(creditCard);
                balanceHistories.add(i + 1, newBalanceHistory);
                i++;
                balanceHistoryRepository.saveAndFlush(newBalanceHistory);
            }
        }
        System.out.println(balanceHistories.size());
    }

    private Integer getClosestPreviousIndex(List<BalanceHistory> balanceHistories, LocalDate target) {
        Integer left = 0;
        Integer right = balanceHistories.size() - 1;
        while (left <= right) {
            Integer mid = (left + right) / 2;
            LocalDate date = balanceHistories.get(mid).getDate();
            if (target.compareTo(date) > 0) {
                left = mid + 1;
            } else if (target.compareTo(date) < 0) {
                right = mid - 1;
            } else {
                return mid;
            }
        }
        return left - 1;
    }

    @PostMapping("/foo")
    public ResponseEntity<Integer> do_foo(@RequestBody Foo[] payload) {
        CreditCard creditCard = new CreditCard();
        creditCard.setIssuanceBank("SECOND");
        creditCard.setNumber("4444");
        creditCardRepository.save(creditCard);
        for (Foo foo : payload) {
            BalanceHistory balanceHistory = new BalanceHistory();
            balanceHistory.setDate(foo.getDate());
            balanceHistory.setBalance(foo.getBalanceAmount());
            balanceHistory.setCreditCard(creditCard);
            balanceHistoryRepository.save(balanceHistory);
        }
        return new ResponseEntity<>(creditCard.getId(), HttpStatus.OK);
    }

    @GetMapping("/check")
    public ResponseEntity<Integer> check(@RequestParam String number) {
        Optional<CreditCard> creditCardData = creditCardRepository
                .findFirstByNumber(number);
        if (!creditCardData.isPresent()) {
            return new ResponseEntity<>(0, HttpStatus.BAD_REQUEST);
        }
        CreditCard creditCard = creditCardData.get();

        for (BalanceHistory balanceHistory : creditCard.getBalanceHistories()) {
            System.out.println(String.format("%s %s", balanceHistory.getDate(), balanceHistory.getBalance()));
        }
        return new ResponseEntity<>(0, HttpStatus.OK);
    }
}
