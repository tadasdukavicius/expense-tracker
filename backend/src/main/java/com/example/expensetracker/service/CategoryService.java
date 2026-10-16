package com.example.expensetracker.service;

import com.example.expensetracker.dto.CategoryRequest;
import com.example.expensetracker.dto.CategoryResponse;
import com.example.expensetracker.entity.Category;
import com.example.expensetracker.exception.ConflictException;
import com.example.expensetracker.exception.NotFoundException;
import com.example.expensetracker.mapper.Mapper;
import com.example.expensetracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repo;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return repo.findAll().stream().map(Mapper::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest r) {
        if (repo.existsByNameIgnoreCase(r.name())) {
            throw new ConflictException("Category already exists: " + r.name());
        }
        Category c = new Category();
        c.setName(r.name());
        c.setColor(r.color());
        return Mapper.toResponse(repo.save(c));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest r) {
        Category c = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
        if (!c.getName().equalsIgnoreCase(r.name()) && repo.existsByNameIgnoreCase(r.name())) {
            throw new ConflictException("Category already exists: " + r.name());
        }
        c.setName(r.name());
        c.setColor(r.color());
        return Mapper.toResponse(c);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Category not found: " + id);
        }
        repo.deleteById(id);
    }
}