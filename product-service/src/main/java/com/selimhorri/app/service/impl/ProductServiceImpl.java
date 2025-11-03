package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.CategoryNotFoundException;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.helper.ProductMappingHelper;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;

	@Override
	public List<ProductDto> findAll() {
		log.info("*** ProductDto List, service; fetch all products *");
		return this.productRepository.findAllWithoutDeleted()
				.stream()
				.map(ProductMappingHelper::map)
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public ProductDto findById(final Integer productId) {
		log.info("*** ProductDto, service; fetch product by id *");
		return this.productRepository.findByIdWithoutDeleted(productId)
				.map(ProductMappingHelper::map)
				.orElseThrow(
						() -> new ProductNotFoundException(String.format("Product with id: %d not found", productId)));
	}

	@Override
	public ProductDto save(final ProductDto productDto) {
		log.info("*** ProductDto, service; save product *");

		// Validar que todos los campos requeridos estén presentes
		if (productDto.getProductTitle() == null || productDto.getProductTitle().isEmpty()) {
			throw new IllegalArgumentException("El título del producto es requerido");
		}

		if (productDto.getImageUrl() == null || productDto.getImageUrl().isEmpty()) {
			throw new IllegalArgumentException("La URL de la imagen es requerida");
		}

		if (productDto.getSku() == null || productDto.getSku().isEmpty()) {
			throw new IllegalArgumentException("El SKU es requerido");
		}

		if (productDto.getPriceUnit() == null) {
			throw new IllegalArgumentException("El precio unitario es requerido");
		}

		if (productDto.getQuantity() == null) {
			throw new IllegalArgumentException("La cantidad es requerida");
		}

		if (productDto.getCategoryDto() == null || productDto.getCategoryDto().getCategoryId() == null) {
			throw new IllegalArgumentException("La categoría es requerida");
		}

		// Confirmar que la categoría asignada existe en BD antes de guardar
		Integer categoryId = productDto.getCategoryDto().getCategoryId();
		categoryRepository.findById(categoryId)
				.orElseThrow(() -> new CategoryNotFoundException("Categoría no encontrada con ID: " + categoryId));

		// Limpiar ID para forzar inserción nueva
		productDto.setProductId(null);
		return ProductMappingHelper.map(this.productRepository
				.save(ProductMappingHelper.map(productDto)));
	}

	@Override
	public ProductDto update(final ProductDto productDto) {
		log.info("*** ProductDto, service; update product *");

		// Asegurar que el producto a actualizar existe en BD
		if (productDto.getProductId() == null || !productRepository.existsById(productDto.getProductId())) {
			throw new ProductNotFoundException("Producto no encontrado con ID: " + productDto.getProductId());
		}
		
		// Confirmar que la categoría asignada existe en BD antes de actualizar
		return ProductMappingHelper.map(this.productRepository
				.save(ProductMappingHelper.map(productDto)));
	}

	@Override
	public ProductDto update(final Integer productId, final ProductDto productDto) {
		log.info("*** ProductDto, service; update product with productId *");

		
		// Obtener el producto existente; lanzar excepción si no existe
		Product existingProduct = productRepository.findById(productId)
				.orElseThrow(() -> new ProductNotFoundException("Producto no encontrado con ID: " + productId));


		// Convertir DTO a entidad y preservar el ID original para la actualización
		Product updatedProduct = ProductMappingHelper.map(productDto);
		updatedProduct.setProductId(existingProduct.getProductId());


		return ProductMappingHelper.map(this.productRepository.save(updatedProduct));
	}

	@Override
	public void deleteById(final Integer productId) {
		log.info("*** Void, service; soft delete product by id *");

		// Verificar que el producto existe (excluyendo los ya eliminados)
		Product product = this.productRepository.findByIdWithoutDeleted(productId)
				.orElseThrow(() -> new ProductNotFoundException("Product with id: " + productId + " not found"));


		// Obtener la categoría "Deleted" usada como marcador para soft delete
		Category deletedCategory = this.categoryRepository.findByCategoryTitle("Deleted")
				.orElseThrow(() -> new RuntimeException("Category 'Deleted' not found in database"));


		// Reasignar el producto a la categoría "Deleted" en lugar de eliminarlo físicamente
		product.setCategory(deletedCategory);
		this.productRepository.save(product);
	}
}
