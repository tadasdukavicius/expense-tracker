package com.example.expensetracker.service;

import com.example.expensetracker.dto.CategoryRequest;
import com.example.expensetracker.dto.CategoryResponse;
import com.example.expensetracker.entity.Category;
import com.example.expensetracker.exception.ConflictException;
import com.example.expensetracker.exception.NotFoundException;
import com.example.expensetracker.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock CategoryRepository repo;
    @InjectMocks CategoryService service;

    @Test
    void createSavesNewCategory() {
        when(repo.existsByNameIgnoreCase("Groceries")).thenReturn(false);
        when(repo.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CategoryResponse result = service.create(new CategoryRequest("Groceries", "#22aa55"));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Groceries");
    }

    @Test
    void createRejectsDuplicateName() {
        when(repo.existsByNameIgnoreCase("Groceries")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CategoryRequest("Groceries", null)))
                .isInstanceOf(ConflictException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void updateMissingCategoryThrowsNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new CategoryRequest("X", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateAllowsKeepingSameNameWithDifferentCase() {
        Category existing = new Category();
        existing.setId(1L);
        existing.setName("Rent");
        when(repo.findById(1L)).thenReturn(Optional.of(existing));

        CategoryResponse result = service.update(1L, new CategoryRequest("rent", "#000000"));

        assertThat(result.name()).isEqualTo("rent");
    }

    @Test
    void deleteMissingCategoryThrowsNotFound() {
        when(repo.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(NotFoundException.class);
        verify(repo, never()).deleteById(any());
    }
}