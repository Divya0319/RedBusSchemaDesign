package com.fastturtle.redbusschemadesign.services;

import com.fastturtle.redbusschemadesign.dtos.CardDTO;
import com.fastturtle.redbusschemadesign.dtos.PaymentRequest;
import com.fastturtle.redbusschemadesign.dtos.PaymentRequestDTO;
import com.fastturtle.redbusschemadesign.enums.CardType;
import com.fastturtle.redbusschemadesign.enums.PaymentRefType;
import com.fastturtle.redbusschemadesign.factories.CardFactorySelector;
import com.fastturtle.redbusschemadesign.helpers.DateUtils;
import com.fastturtle.redbusschemadesign.models.*;
import com.fastturtle.redbusschemadesign.enums.PaymentMethod;
import com.fastturtle.redbusschemadesign.enums.PaymentStatus;
import com.fastturtle.redbusschemadesign.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final BookingRepository bookingRepository;
    private final CardDetailRepository cardDetailRepository;
    private final UserWalletRepository userWalletRepository;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final BankDetailRepository bankDetailRepository;
    private final CardFactorySelector cardFactorySelector;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, BookingRepository bookingRepository, CardDetailRepository cardDetailRepository, UserWalletRepository userWalletRepository, UserRepository userRepository, BankAccountRepository bankAccountRepository, BankDetailRepository bankDetailRepository, CardFactorySelector cardFactorySelector) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.cardDetailRepository = cardDetailRepository;
        this.userWalletRepository = userWalletRepository;
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.bankDetailRepository = bankDetailRepository;
        this.cardFactorySelector = cardFactorySelector;
    }

    public ResponseEntity<?> makePayment(PaymentRequest paymentRequest) {
        Optional<Booking> booking = bookingRepository.findById(paymentRequest.getBookingId());

        if(booking.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Booking not found");
        } else {
            Payment payment = new Payment();
            payment.setBooking(booking.get());
            payment.setAmount(paymentRequest.getAmount());
            payment.setPaymentMethod(paymentRequest.getPaymentMethod());

            return ResponseEntity.ok().body(paymentRepository.save(payment));
        }

    }

    public ResponseEntity<?> updatePayment(PaymentRequest paymentRequest) {
        Optional<Payment> payment = paymentRepository.findByBookingId(paymentRequest.getBookingId());
        if(payment.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment not found for booking id " + paymentRequest.getBookingId());
        } else {
            Payment p = payment.get();
            p.setAmount(paymentRequest.getAmount());
            p.setPaymentMethod(paymentRequest.getPaymentMethod());
            p.setPaymentStatus(paymentRequest.getPaymentStatus());

            return ResponseEntity.ok().body(paymentRepository.save(p));

        }

    }

    public ResponseEntity<?> getPaymentStatus(int bookingId) {
        Optional<Payment> payment = paymentRepository.findByBookingId(bookingId);
        if(payment.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment not found for booking id " + bookingId);
        } else {
            return ResponseEntity.ok().body(payment.get().getPaymentStatus());
        }

    }

    public void processPayment(PaymentRequestDTO paymentRequestDTO) {

        // Determine the payment type
        switch (paymentRequestDTO.getPaymentRefType()) {
            case CARD:
                // Process card payment
                processCardPayment(paymentRequestDTO);
                break;
            case BANK:
                // Process net banking payment
                processBankPayment(paymentRequestDTO);
                break;
            case USER:
                // Process wallet payment
                processWalletPayment(paymentRequestDTO);
                break;
        }

    }

    private void processCardPayment(PaymentRequestDTO dto) {
        Booking booking = bookingRepository.findByBookingId(dto.getBookingId()).orElse(null);
        Payment payment = booking.getPayment();
        if(dto.getPaymentMode() == PaymentMethod.DEBIT_CARD) {
            payment.setPaymentMethod(PaymentMethod.DEBIT_CARD);
        } else {
            payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        }

        if(dto.getAction().equals("completed")) {
            payment.setPaymentStatus(PaymentStatus.COMPLETED);
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
        }

        CardType cardType = dto.getPaymentMode() == PaymentMethod.DEBIT_CARD ? CardType.DEBIT : CardType.CREDIT;

        CardDetails cardDetails = cardDetailRepository.findByCardNumberAndCardType(dto.getCardNo(), cardType);
        if(cardDetails == null) {
            User user = userRepository.findById(dto.getUserID()).get();

            if(cardType == CardType.DEBIT) {
                List<BankAccount> bankAccounts = bankAccountRepository.findAll();
                CardDTO cardDTO = convertToCardDTO(dto);
                cardDetails = cardFactorySelector.createCard(cardDTO, cardType, bankAccounts);

            } else {
                List<BankDetails> banks = bankDetailRepository.findAll();
                CardDTO cardDTO = convertToCardDTO(dto);
                cardDetails = cardFactorySelector.createCard(cardDTO, cardType, banks);

            }
            cardDetails.setLinkedUser(user);
            cardDetailRepository.save(cardDetails);
            payment.setPaymentReferenceId(cardDetails.getCardId());

        } else {

            payment.setPaymentReferenceId(cardDetails.getCardId());
        }
        payment.setPaymentReferenceType(PaymentRefType.CARD);
        payment.setAmount(booking.getPrice());
        payment.setPaymentDate(LocalDate.now());

        paymentRepository.save(payment);

    }

    private CardDTO convertToCardDTO(PaymentRequestDTO dto) {
        CardDTO cardDTO = new CardDTO();
        cardDTO.setCardNumber(dto.getCardNo());
        cardDTO.setCardHolderName(dto.getCardHolderName());
        cardDTO.setCardCompany(dto.getCardCompany());
        cardDTO.setExpiryMonth(dto.getExpiryMonth());
        cardDTO.setExpiryYear(dto.getExpiryYear());
        cardDTO.setCvv(dto.getCvv());

        return cardDTO;
    }

    private void processBankPayment(PaymentRequestDTO dto) {
        Booking booking = bookingRepository.findByBookingId(dto.getBookingId()).orElse(null);
        Payment payment = booking.getPayment();
        payment.setPaymentMethod(PaymentMethod.NETBANKING);

        if(dto.getAction().equals("completed")) {
            payment.setPaymentStatus(PaymentStatus.COMPLETED);
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
        }

        payment.setPaymentReferenceId(dto.getBankID());
        payment.setPaymentReferenceType(PaymentRefType.BANK);
        payment.setAmount(booking.getPrice());
        payment.setPaymentDate(LocalDate.now());

        paymentRepository.save(payment);

    }

    private void processWalletPayment(PaymentRequestDTO dto) {
        Booking booking = bookingRepository.findByBookingId(dto.getBookingId()).orElse(null);
        Payment payment = booking.getPayment();
        payment.setPaymentMethod(PaymentMethod.WALLET);
        if(dto.getAction().equals("completed")) {
            payment.setPaymentStatus(PaymentStatus.COMPLETED);

            UserWallet userWallet = userWalletRepository.findByUserId(dto.getUserID());
            userWallet.setBalance(userWallet.getBalance().subtract(BigDecimal.valueOf(booking.getPrice())));
            userWalletRepository.save(userWallet);

        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
        }
        payment.setPaymentReferenceId(dto.getUserID());
        payment.setPaymentReferenceType(PaymentRefType.USER);
        payment.setAmount(booking.getPrice());
        payment.setPaymentDate(LocalDate.now());

        User user = userRepository.findById(dto.getUserID()).get();
        payment.setUser(user);

        paymentRepository.save(payment);
    }

    public ResponseEntity<?> checkPaymentStatusAndReturnBookingForBookingId(int bookingId) {
        ResponseEntity<?> response;
        Optional<Booking> booking = bookingRepository.findByBookingId(bookingId);

        if(booking.isEmpty()) {
            response = ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Booking not found for booking id " + bookingId));
            return response;
        } else {
            Optional<Payment> optionalPayment = paymentRepository.findByBookingId(bookingId);

            // Formatted dates with ordinal suffixes
            String formattedBookingDate = DateUtils.formatWithOrdinalSuffix(booking.get().getBookingDate());
            String formattedTravelDate = DateUtils.formatWithOrdinalSuffix(booking.get().getTravelDate());

            booking.get().setFormattedBookingDate(formattedBookingDate);
            booking.get().setFormattedTravelDate(formattedTravelDate);

            Payment payment = optionalPayment.get();

            if(payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
                Float paidAmt = payment.getAmount();
                PaymentMethod chosenPaymentMode = payment.getPaymentMethod();
                response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        Map.of(
                                "warning", "Payment is Already Completed for booking id " + bookingId,
                                "paidAmt", paidAmt,
                                "chosenPaymentMode", chosenPaymentMode));
                return response;
            }

            response = ResponseEntity.ok(booking.get());
        }

        return response;

    }
}
