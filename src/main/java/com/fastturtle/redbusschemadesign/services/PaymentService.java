package com.fastturtle.redbusschemadesign.services;

import com.fastturtle.redbusschemadesign.dtos.PaymentRequest;
import com.fastturtle.redbusschemadesign.models.Booking;
import com.fastturtle.redbusschemadesign.models.Payment;
import com.fastturtle.redbusschemadesign.models.PaymentStatus;
import com.fastturtle.redbusschemadesign.repositories.BookingRepository;
import com.fastturtle.redbusschemadesign.repositories.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final BookingRepository bookingRepository;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
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
}
