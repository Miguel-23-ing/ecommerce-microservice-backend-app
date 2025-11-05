package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.DuplicateEntityException;
import com.selimhorri.app.exception.wrapper.FavouriteNotFoundException;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.exception.wrapper.UserNotFoundException;
import com.selimhorri.app.repository.FavouriteRepository;

@ExtendWith(MockitoExtension.class)
class FavouriteServiceImplTest {

    @Mock
    private FavouriteRepository favouriteRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FavouriteServiceImpl favouriteService;

    private Favourite favourite;
    private FavouriteDto favouriteDto;
    private FavouriteId favouriteId;
    private UserDto userDto;
    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        LocalDateTime likeDate = LocalDateTime.now();
        
        favouriteId = new FavouriteId();
        favouriteId.setUserId(7);
        favouriteId.setProductId(22);
        favouriteId.setLikeDate(likeDate);
        
        favourite = new Favourite();
        favourite.setUserId(7);
        favourite.setProductId(22);
        favourite.setLikeDate(likeDate);
        
        userDto = UserDto.builder()
                .userId(7)
                .firstName("Carlos")
                .lastName("Martinez")
                .email("carlos.martinez@test.com")
                .build();
        
        productDto = ProductDto.builder()
                .productId(22)
                .productTitle("Monitor UltraWide 34 pulgadas")
                .priceUnit(599.99)
                .build();
        
        favouriteDto = FavouriteDto.builder()
                .userId(7)
                .productId(22)
                .likeDate(likeDate)
                .userDto(userDto)
                .productDto(productDto)
                .build();
    }

    @Test
    void findAll_ShouldReturnListOfFavourites() {
        // Arrange
        when(favouriteRepository.findAll()).thenReturn(Collections.singletonList(favourite));
        when(restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/7", UserDto.class))
                .thenReturn(userDto);
        when(restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/22", ProductDto.class))
                .thenReturn(productDto);

        // Act
        List<FavouriteDto> result = favouriteService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(favouriteDto.getUserId(), result.get(0).getUserId());
        assertEquals(favouriteDto.getProductId(), result.get(0).getProductId());
        
        verify(favouriteRepository, times(1)).findAll();
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/7", UserDto.class);
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/22", ProductDto.class);
    }

    @Test
    void findAll_ShouldFilterNullResults() {
        // Arrange
        when(favouriteRepository.findAll()).thenReturn(Collections.singletonList(favourite));
        when(restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/7", UserDto.class))
                .thenReturn(null); // Simulate user not found

        // Act
        List<FavouriteDto> result = favouriteService.findAll();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findById_ShouldReturnFavourite() {
        // Arrange
        when(favouriteRepository.findByUserIdAndProductId(7, 22))
                .thenReturn(Optional.of(favourite));
        when(restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/7", UserDto.class))
                .thenReturn(userDto);
        when(restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/22", ProductDto.class))
                .thenReturn(productDto);

        // Act
        FavouriteDto result = favouriteService.findById(favouriteId);

        // Assert
        assertNotNull(result);
        assertEquals(favouriteDto.getUserId(), result.getUserId());
        assertEquals(favouriteDto.getProductId(), result.getProductId());
        assertNotNull(result.getUserDto());
        assertNotNull(result.getProductDto());
    }

    @Test
    void findById_ShouldThrowFavouriteNotFoundException() {
        // Arrange
        when(favouriteRepository.findByUserIdAndProductId(7, 22))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(FavouriteNotFoundException.class, () -> {
            favouriteService.findById(favouriteId);
        });
    }

    @Test
    void findById_ShouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(favouriteRepository.findByUserIdAndProductId(7, 22))
                .thenReturn(Optional.of(favourite));
        when(restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/7", UserDto.class))
                .thenReturn(null);

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            favouriteService.findById(favouriteId);
        });
    }

    @Test
    void findById_ShouldThrowExceptionWhenProductNotFound() {
        // Arrange
        when(favouriteRepository.findByUserIdAndProductId(7, 22))
                .thenReturn(Optional.of(favourite));
        when(restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/7", UserDto.class))
                .thenReturn(userDto);
        when(restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/22", ProductDto.class))
                .thenReturn(null);

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> {
            favouriteService.findById(favouriteId);
        });
    }

    @Test
    void save_ShouldSaveFavourite() {
        // Arrange
        when(restTemplate.getForEntity(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/7", UserDto.class))
                .thenReturn(new ResponseEntity<>(userDto, HttpStatus.OK));
        when(restTemplate.getForEntity(
                AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/22", ProductDto.class))
                .thenReturn(new ResponseEntity<>(productDto, HttpStatus.OK));
        when(favouriteRepository.existsByUserIdAndProductId(7, 22)).thenReturn(false);
        when(favouriteRepository.save(any(Favourite.class))).thenReturn(favourite);

        // Act
        FavouriteDto result = favouriteService.save(favouriteDto);

        // Assert
        assertNotNull(result);
        assertEquals(favouriteDto.getUserId(), result.getUserId());
        assertEquals(favouriteDto.getProductId(), result.getProductId());
        
        verify(favouriteRepository, times(1)).save(any(Favourite.class));
    }

    @Test
    void save_ShouldThrowUserNotFoundException() {
        // Arrange
        when(restTemplate.getForEntity(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/7", UserDto.class))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            favouriteService.save(favouriteDto);
        });
    }

    @Test
    void save_ShouldThrowProductNotFoundException() {
        // Arrange
        when(restTemplate.getForEntity(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/7", UserDto.class))
                .thenReturn(new ResponseEntity<>(userDto, HttpStatus.OK));
        when(restTemplate.getForEntity(
                AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/22", ProductDto.class))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> {
            favouriteService.save(favouriteDto);
        });
    }

    @Test
    void save_ShouldThrowDuplicateEntityException() {
        // Arrange
        when(restTemplate.getForEntity(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/7", UserDto.class))
                .thenReturn(new ResponseEntity<>(userDto, HttpStatus.OK));
        when(restTemplate.getForEntity(
                AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/22", ProductDto.class))
                .thenReturn(new ResponseEntity<>(productDto, HttpStatus.OK));
        when(favouriteRepository.existsByUserIdAndProductId(7, 22)).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateEntityException.class, () -> {
            favouriteService.save(favouriteDto);
        });
    }

    @Test
    void deleteById_ShouldDeleteFavourite() {
        // Arrange
        when(favouriteRepository.existsByUserIdAndProductId(7, 22)).thenReturn(true);
        doNothing().when(favouriteRepository).deleteByUserIdAndProductId(7, 22);

        // Act
        assertDoesNotThrow(() -> {
            favouriteService.deleteById(favouriteId);
        });

        // Assert
        verify(favouriteRepository, times(1)).deleteByUserIdAndProductId(7, 22);
    }

    @Test
    void deleteById_ShouldThrowFavouriteNotFoundException() {
        // Arrange
        when(favouriteRepository.existsByUserIdAndProductId(7, 22)).thenReturn(false);

        // Act & Assert
        assertThrows(FavouriteNotFoundException.class, () -> {
            favouriteService.deleteById(favouriteId);
        });
    }
}
