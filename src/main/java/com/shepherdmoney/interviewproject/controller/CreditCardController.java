package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.repository.BalanceHistoryRepository;

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
        // SEARCH USER
        Optional<User> userData = userRepository.findById(payload.getUserId());
        if (!userData.isPresent()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        User user = userData.get();

        // CREATE CREDIT CARD AND ASSOCIATE CARD TO USER
        CreditCard creditCard = new CreditCard();
        creditCard.setIssuanceBank(payload.getCardIssuanceBank());
        creditCard.setNumber(payload.getCardNumber());
        creditCard.setUser(user);

        // SAVE
        creditCardRepository.save(creditCard);
        return new ResponseEntity<>(creditCard.getId(), HttpStatus.OK);
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // SEARCH USER
        Optional<User> userData = userRepository.findById(userId);
        if (!userData.isPresent()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // GET LIST OF CREDIT CARDS
        List<CreditCardView> creditCards = new ArrayList<>();
        userData.get().getCreditCards().forEach(creditCard -> {
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

        // SEARCH CREDIT CARD
        Optional<CreditCard> creditCardData = creditCardRepository.findFirstByNumber(creditCardNumber);
        if (!creditCardData.isPresent()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        CreditCard creditCard = creditCardData.get();

        // CHECK IF USER EXISTS
        Integer userId = creditCard.getUser().getId();
        if (userId == null || !userRepository.existsById(userId)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(userId, HttpStatus.OK);
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<Integer> postMethodName(@RequestBody UpdateBalancePayload[] payload) {
        for (UpdateBalancePayload updateBalancePayload : payload) {
            // CHECK IF CREDIT CARD EXISTS
            Optional<CreditCard> creditCardData = creditCardRepository
                    .findFirstByNumber(updateBalancePayload.getCreditCardNumber());
            if (!creditCardData.isPresent()) {
                continue;
            }
            CreditCard creditCard = creditCardData.get();
            List<BalanceHistory> balanceHistories = creditCard.getBalanceHistories();

            // GET CLOSEST PREVIOIUS INDEX USING BINARY SEARCH
            Integer closestPreviousIndex = getClosestPreviousIndex(balanceHistories,
                    updateBalancePayload.getBalanceDate());

            // IF UPDATE HAS A PREVIOUS INDEX THEN PROPAGATE DIFFERENCE
            if (closestPreviousIndex != -1) {
                double difference = updateBalancePayload.getBalanceAmount()
                        - balanceHistories.get(closestPreviousIndex).getBalance();
                for (int i = closestPreviousIndex + 1; i < balanceHistories.size(); i++) {
                    balanceHistories.get(i).setBalance(balanceHistories.get(i).getBalance() + difference);
                }
            }

            // IF UPDATE DATE ALREADY EXISTS, JUST PATCH AND SAVE
            if (closestPreviousIndex != -1
                    && balanceHistories.get(closestPreviousIndex).getDate().compareTo(
                            updateBalancePayload.getBalanceDate()) == 0) {
                balanceHistories.get(closestPreviousIndex).setBalance(updateBalancePayload.getBalanceAmount());
                balanceHistoryRepository.saveAndFlush(balanceHistories.get(closestPreviousIndex));
            } else {
                // UPDATE DATE DOES NOT EXIST, MAKE NEW ONE AND INSERT.
                BalanceHistory newBalanceHistory = new BalanceHistory();
                newBalanceHistory.setDate(updateBalancePayload.getBalanceDate());
                newBalanceHistory.setBalance(updateBalancePayload.getBalanceAmount());
                newBalanceHistory.setCreditCard(creditCard);
                balanceHistories.add(closestPreviousIndex + 1, newBalanceHistory);
                balanceHistoryRepository.saveAndFlush(newBalanceHistory);
            }
            // FILL GAP FROM EARLIEST ENTRY TO TODAYS DATE.
            // ASSUMPTION THAT "GAP" REFERS TO THE EARLIEST ENTRY TO THE CURRENT ONE.
            fillUntilToday(balanceHistories, creditCard);
        }
        return new ResponseEntity<>(payload.length, HttpStatus.OK);
    }

    private void fillUntilToday(List<BalanceHistory> balanceHistories, CreditCard creditCard) {
        // FOR EVERY GAP, FILL WITH THE BALANCE OF PREVIOUS DATE, STARTING FROM THE
        // SECOND EARLIEST DATE
        Integer i = 0;
        LocalDate date = balanceHistories.get(i).getDate();
        Double balance = balanceHistories.get(i).getBalance();

        // WHILE DATE TO CHECK IS BEFORE TODAYS DATE
        while (LocalDate.now().compareTo(date) > 0) {
            date = date.plusDays(1);
            if (i < balanceHistories.size() - 1 && date.compareTo(balanceHistories.get(i + 1).getDate()) == 0) {
                // CHECK FOR NEW GAP. SET BALANCE TO BE FROM THE PREVIOUS DATE
                i++;
                date = balanceHistories.get(i).getDate();
                balance = balanceHistories.get(i).getBalance();
            } else {
                // FILL GAP. INSERT TO LIST AND SAVE
                BalanceHistory newBalanceHistory = new BalanceHistory();
                newBalanceHistory.setDate(date);
                newBalanceHistory.setBalance(balance);
                newBalanceHistory.setCreditCard(creditCard);
                balanceHistories.add(i + 1, newBalanceHistory);
                i++;
                balanceHistoryRepository.saveAndFlush(newBalanceHistory);
            }
        }
    }

    private Integer getClosestPreviousIndex(List<BalanceHistory> balanceHistories, LocalDate target) {
        // BINARY SEARCH
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
}
