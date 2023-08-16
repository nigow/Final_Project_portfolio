package org.example.stockmarket.stocks.earnings.repository;

import org.example.stockmarket.stocks.earnings.entity.EarningsReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EarningsRepository extends JpaRepository<EarningsReport, Long> {
}
