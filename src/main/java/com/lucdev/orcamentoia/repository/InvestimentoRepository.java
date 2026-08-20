package com.lucdev.orcamentoia.repository;

import com.lucdev.orcamentoia.model.Investimento;
import com.lucdev.orcamentoia.model.TipoInvestimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestimentoRepository extends JpaRepository<Investimento, Long> {

    List<Investimento> findByTipo(TipoInvestimento tipo);
}
