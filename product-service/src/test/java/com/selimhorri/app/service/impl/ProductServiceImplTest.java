package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setCategoryId(1);
        testCategory.setCategoryTitle("Electronics");

        testProduct = new Product();
        testProduct.setProductId(3);
        testProduct.setProductTitle("Mouse Inalámbrico Logitech");
        testProduct.setPriceUnit(45.99);
        testProduct.setImageUrl("http://example.com/mouse.jpg");
        testProduct.setSku("MOUSE-LOGI-001");
        testProduct.setQuantity(50);
        testProduct.setCategory(testCategory);
    }

    @Test
    void findAll_shouldReturnListOfProducts() {
        when(productRepository.findAllWithoutDeleted()).thenReturn(List.of(testProduct));

        List<ProductDto> result = productService.findAll();

        assertEquals(1, result.size());
        assertEquals("Mouse Inalámbrico Logitech", result.get(0).getProductTitle());
        verify(productRepository, times(1)).findAllWithoutDeleted();
    }

    @Test
    void findAll_shouldReturnEmptyListWhenNoProducts() {
        when(productRepository.findAllWithoutDeleted()).thenReturn(Collections.emptyList());

        List<ProductDto> result = productService.findAll();

        assertEquals(0, result.size());
        verify(productRepository, times(1)).findAllWithoutDeleted();
    }

    @Test
    void findById_shouldReturnProductWhenFound() {
        when(productRepository.findByIdWithoutDeleted(3)).thenReturn(Optional.of(testProduct));

        ProductDto result = productService.findById(3);

        assertNotNull(result);
        assertEquals(3, result.getProductId());
        assertEquals("Mouse Inalámbrico Logitech", result.getProductTitle());
        assertEquals(45.99, result.getPriceUnit());
        verify(productRepository, times(1)).findByIdWithoutDeleted(3);
    }

    @Test
    void findById_shouldThrowExceptionWhenNotFound() {
        when(productRepository.findByIdWithoutDeleted(99)).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
            ProductNotFoundException.class, 
            () -> productService.findById(99)
        );
        
        assertTrue(exception.getMessage().contains("Product with id: 99 not found"));
        verify(productRepository, times(1)).findByIdWithoutDeleted(99);
    }

    @Test
    void save_shouldThrowExceptionWhenImageUrlIsNull() {
        ProductDto productDto = ProductDto.builder()
                .productTitle("Teclado Mecánico")
                .imageUrl(null)
                .sku("TECH-KB-001")
                .priceUnit(89.99)
                .quantity(20)
                .categoryDto(CategoryDto.builder().categoryId(1).build())
                .build();

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> productService.save(productDto)
        );

        assertEquals("La URL de la imagen es requerida", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void save_shouldThrowExceptionWhenSkuIsNull() {
        ProductDto productDto = ProductDto.builder()
                .productTitle("Teclado Mecánico")
                .imageUrl("http://example.com/keyboard.jpg")
                .sku(null)
                .priceUnit(89.99)
                .quantity(20)
                .categoryDto(CategoryDto.builder().categoryId(1).build())
                .build();

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> productService.save(productDto)
        );

        assertEquals("El SKU es requerido", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteById_shouldMarkProductAsDeleted() {
        Category deletedCategory = new Category();
        deletedCategory.setCategoryId(999);
        deletedCategory.setCategoryTitle("Deleted");

        when(productRepository.findByIdWithoutDeleted(3)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findByCategoryTitle("Deleted")).thenReturn(Optional.of(deletedCategory));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        productService.deleteById(3);

        verify(productRepository, times(1)).findByIdWithoutDeleted(3);
        verify(categoryRepository, times(1)).findByCategoryTitle("Deleted");
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void deleteById_shouldThrowExceptionWhenProductNotFound() {
        when(productRepository.findByIdWithoutDeleted(99)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.deleteById(99));
        verify(productRepository, never()).save(any());
    }
}
