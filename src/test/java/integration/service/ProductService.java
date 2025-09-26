package integration.service;

import integration.entity.Product;
import integration.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void deductStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        if (product.getStock() < quantity) {
            throw new IllegalStateException("Not enough stock for product " + productId);
        }

        product.setStock(product.getStock() - quantity);
    }
}
