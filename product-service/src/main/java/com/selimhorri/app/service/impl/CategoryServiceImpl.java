package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.exception.wrapper.CategoryNotFoundException;
import com.selimhorri.app.helper.CategoryMappingHelper;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.CategoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;

	@Override
	public List<CategoryDto> findAll() {
		return this.categoryRepository.findAllNonReserved()
				.stream()
				.map(CategoryMappingHelper::map)
				.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public CategoryDto findById(final Integer categoryId) {
		return this.categoryRepository.findNonReservedById(categoryId)
				.map(CategoryMappingHelper::map)
				.orElseThrow(() -> new CategoryNotFoundException(
						String.format("Category with id: %d not found or is reserved", categoryId)));
	}

	@Override
	@Transactional
	public CategoryDto save(final CategoryDto categoryDto) {
		log.info("*** CategoryDto, service; save category *");

		if (categoryDto.getCategoryTitle() == null || categoryDto.getCategoryTitle().trim().isEmpty()) {
			throw new IllegalArgumentException("Category title cannot be empty or null");
		}

		String normalizedTitle = categoryDto.getCategoryTitle().trim();

		boolean nameExists = this.categoryRepository.existsByCategoryTitleIgnoreCase(normalizedTitle);
		if (nameExists) {
			throw new IllegalArgumentException("A category with this name already exists");
		}

		// Limpiar relaciones e ID antes de persistir para forzar inserción nueva
		categoryDto.setParentCategoryDto(null);
		categoryDto.setSubCategoriesDtos(null);
		categoryDto.setCategoryId(null);

		// Convertir DTO a entidad, guardar en BD y retornar como DTO
		return CategoryMappingHelper.map(
				this.categoryRepository.save(CategoryMappingHelper.map(categoryDto)));
	}

	@Override
	@Transactional
	public CategoryDto update(final CategoryDto categoryDto) {
		log.info("*** CategoryDto, service; update category *");

		// Asegurar que el ID esté presente para identificar el registro a actualizar
		if (categoryDto.getCategoryId() == null) {
			throw new IllegalArgumentException("Category ID cannot be null for update");
		}

		if (categoryDto.getCategoryTitle() == null || categoryDto.getCategoryTitle().trim().isEmpty()) {
			throw new IllegalArgumentException("Category title cannot be empty or null");
		}

		String normalizedTitle = categoryDto.getCategoryTitle().trim();

		// Obtener categoría existente; lanzar excepción si no existe
		Category existingCategory = this.categoryRepository.findById(categoryDto.getCategoryId())
				.orElseThrow(() -> new CategoryNotFoundException(
						"Category not found with ID: " + categoryDto.getCategoryId()));

		// Verificar que ninguna otra categoría use el mismo nombre (evitar duplicados)
		boolean nameExists = this.categoryRepository.existsByCategoryTitleIgnoreCaseAndCategoryIdNot(
				normalizedTitle, categoryDto.getCategoryId());

		if (nameExists) {
			throw new IllegalArgumentException("Another category with this name already exists");
		}

		// Actualizar el nombre y limpiar referencias
		existingCategory.setCategoryTitle(normalizedTitle);
		existingCategory.setParentCategory(null);
		existingCategory.setSubCategories(null);

		return CategoryMappingHelper.map(this.categoryRepository.save(existingCategory));
	}

	@Override
	@Transactional
	public CategoryDto update(final Integer categoryId, final CategoryDto categoryDto) {
		log.info("*** CategoryDto, service; update category with categoryId *");

		// Validar que el ID del path sea válido
		if (categoryId == null) {
			throw new IllegalArgumentException("Category ID cannot be null");
		}

		if (categoryDto.getCategoryTitle() == null || categoryDto.getCategoryTitle().trim().isEmpty()) {
			throw new IllegalArgumentException("Category title cannot be empty or null");
		}

		String normalizedTitle = categoryDto.getCategoryTitle().trim();

		// Obtener categoría existente; lanzar excepción si no existe
		Category existingCategory = this.categoryRepository.findById(categoryId)
				.orElseThrow(() -> new CategoryNotFoundException("Category not found with ID: " + categoryId));

		// Verificar que ninguna otra categoría use el mismo nombre (evitar duplicados)
		boolean nameExists = this.categoryRepository.existsByCategoryTitleIgnoreCaseAndCategoryIdNot(
				normalizedTitle, categoryId);

		if (nameExists) {
			throw new IllegalArgumentException("Another category with this name already exists");
		}

		// Actualizar el nombre y limpiar referencias
		existingCategory.setCategoryTitle(normalizedTitle);
		existingCategory.setParentCategory(null);
		existingCategory.setSubCategories(null);

		// Trabajar con entidad existente evita conflictos de mapeo
		return CategoryMappingHelper.map(this.categoryRepository.save(existingCategory));
	}

	@Override
	@Transactional
	public void deleteById(final Integer categoryId) {
		log.info("*** Void, service; delete category by id *");

		// Validar que la categoría exista
		Category category = this.categoryRepository.findById(categoryId)
				.orElseThrow(() -> new CategoryNotFoundException(
						"Category not found with ID: " + categoryId));

		// Impedir eliminación de categorías reservadas (usadas internamente)
		String categoryName = category.getCategoryTitle().toLowerCase().trim();
		if ("deleted".equals(categoryName) || "no category".equals(categoryName)) {
			throw new IllegalArgumentException(
					"Cannot delete reserved categories: 'Deleted' or 'No Category'");
		}

		// Obtener categoría por defecto "No Category" para reasignación de productos
		Category noCategory = this.categoryRepository.findByCategoryTitleIgnoreCase("No Category")
				.orElseThrow(() -> new IllegalStateException(
						"The 'No Category' category is required but not found in database"));

		// Mover todos los productos de esta categoría a "No Category" antes de eliminar
		this.productRepository.updateCategoryForProducts(categoryId, noCategory);

		// Finalmente, eliminar la categoría vacía
		this.categoryRepository.delete(category);
	}

}
