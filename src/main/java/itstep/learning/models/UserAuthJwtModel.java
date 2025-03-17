package itstep.learning.models;

import itstep.learning.dal.dto.Cart;
import itstep.learning.dal.dto.User;

public class UserAuthJwtModel {
    private User user;
    private String jwtToken;
    private Cart cart;

    public UserAuthJwtModel() {
    }

    public UserAuthJwtModel(User user, String jwtToken, Cart cart) {
        this.user = user;
        this.jwtToken = jwtToken;
        this.cart = cart;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }    

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }
    
}
