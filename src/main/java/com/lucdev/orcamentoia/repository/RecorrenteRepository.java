package com.lucdev.orcamentoia.repository;

import com.lucdev.orcamentoia.model.Recorrente;
import com.lucdev.orcamentoia.model.TipoTransacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecorrenteRepository extends JpaRepository<Recorrente, Long> {

    List<Recorrente> findAllByTipoOrderByDiaVencimentoAsc(TipoTransacao tipo);

    List<Recorrente> findAllByOrderByTipoAscDiaVencimentoAsc();
}
