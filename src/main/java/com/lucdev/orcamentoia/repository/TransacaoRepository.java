package com.lucdev.orcamentoia.repository;

import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    List<Transacao> findByTipo(TipoTransacao tipo);

    List<Transacao> findByCategoriaIgnoreCase(String categoria);
}
