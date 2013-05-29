package test.unit.org.testinfected.petstore.controllers;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testinfected.molecule.Session;
import org.testinfected.petstore.controllers.CreateCartItem;
import org.testinfected.petstore.order.Cart;
import org.testinfected.petstore.order.CartItem;
import org.testinfected.petstore.product.Item;
import org.testinfected.petstore.product.ItemInventory;
import org.testinfected.petstore.product.ItemNumber;
import test.support.org.testinfected.molecule.unit.MockRequest;
import test.support.org.testinfected.molecule.unit.MockResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static test.support.org.testinfected.molecule.unit.MockRequest.aRequest;
import static test.support.org.testinfected.molecule.unit.MockResponse.aResponse;
import static test.support.org.testinfected.petstore.builders.ItemBuilder.anItem;

@RunWith(JMock.class)
public class CreateCartItemTest {

    Mockery context = new JUnit4Mockery();
    ItemInventory inventory = context.mock(ItemInventory.class);
    CreateCartItem createCartItem = new CreateCartItem(inventory);

    MockRequest request = aRequest();
    MockResponse response = aResponse();

    String itemNumber = "12345678";

    @SuppressWarnings("unchecked") @Test public void
    createsCartAndAddsItemToCartBeforeRedirectingToCartPage() throws Exception {
        request.addParameter("item-number", itemNumber);
        final Item item = anItem().withNumber(itemNumber).build();

        context.checking(new Expectations() {{
            allowing(inventory).find(new ItemNumber(itemNumber)); will(returnValue(item));
        }});

        createCartItem.handle(request, response);

        response.assertRedirectedTo("/cart");
        assertThat("session", session(), notNullValue());
        assertThat("cart content", cart().getItems(), containsItems(itemWith(number(itemNumber), quantity(1))));
    }

    private Session session() {
        return request.session(false);
    }

    private Cart cart() {
        return (Cart) request.session().get(Cart.class);
    }

    private Matcher<Iterable<CartItem>> containsItems(Matcher<? super CartItem>... cartItemMatchers) {
        return hasItems(cartItemMatchers);
    }

    private Matcher<CartItem> itemWith(Matcher<CartItem>... itemMatchers) {
        return allOf(itemMatchers);
    }

    private Matcher<CartItem> quantity(int count) {
        return new FeatureMatcher<CartItem, Integer>(equalTo(count), "an item with quantity", "item quantity") {
            @Override protected Integer featureValueOf(CartItem actual) {
                return actual.getQuantity();
            }
        };
    }

    private Matcher<CartItem> number(String number) {
        return new FeatureMatcher<CartItem, String>(equalTo(number), "an item with number", "item number") {
            @Override protected String featureValueOf(CartItem actual) {
                return actual.getItemNumber();
            }
        };
    }
}
