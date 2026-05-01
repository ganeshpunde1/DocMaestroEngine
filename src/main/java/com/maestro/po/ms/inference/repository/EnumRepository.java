package com.maestro.po.ms.inference.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface EnumRepository<T, I> extends JpaRepository<T, I>, JpaSpecificationExecutor<T>
{

    List<T> findByValue(String name);

}
