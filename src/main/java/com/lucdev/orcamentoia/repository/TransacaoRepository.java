package com.lucdev.orcamentoia.repository;

import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    // Lancamentos vindos de itens fixos, para saber o que ja foi pago no mes.
    java.util.List<Transacao> findByRecorrenteIdIsNotNull();

    List<Transacao> findByTipo(TipoTransacao tipo);

    List<Transacao> findByCategoriaIgnoreCase(String categoria);
}
