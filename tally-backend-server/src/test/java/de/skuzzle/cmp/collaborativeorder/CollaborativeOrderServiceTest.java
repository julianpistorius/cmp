package de.skuzzle.cmp.collaborativeorder;

import static de.skuzzle.cmp.collaborativeorder.Amount.times;
import static de.skuzzle.cmp.collaborativeorder.Money.money;
import static de.skuzzle.cmp.collaborativeorder.Percentage.percent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CollaborativeOrderServiceTest {

    private static final UserId SIMON = UserId.wellKnown("google", "Simon");
    private static final UserId ROBERT = UserId.wellKnown("google", "Robert");
    private static final UserId TIMO = UserId.wellKnown("google", "Timo");

    private final LineItem pizzaSalami = LineItem.singleProductWithName("Pizza Salami", priced(10));
    private final LineItem bread = LineItem.multipleProductsWithName("Pizza Brötchen", priced(5), times(2));
    private final LineItem pizzaTuna = LineItem.singleProductWithName("Pizza Tuna", priced(15));

    @Autowired
    private CollaborativeOrderService orders;

    private CollaborativeOrder organizeSampleOrder() {
        CollaborativeOrder order = orders.organizeCollaborativeOrder("Domino's", SIMON);
        orders.join(order.getId(), SIMON);
        orders.join(order.getId(), ROBERT);

        order = orders.addLineItem(order.getId(), SIMON, pizzaTuna);
        order = orders.addLineItem(order.getId(), SIMON, bread);
        order = orders.addLineItem(order.getId(), ROBERT, pizzaSalami);

        return order;
    }

    @Test
    void testOrderNoTipNoDiscount() throws Exception {
        final CollaborativeOrder order = organizeSampleOrder();

        final CalculatedPrices totals = order.getCalculatedPrices();
        totals.checkConsistency();
        assertThat(totals.getOriginalPrice()).isEqualTo(money(35));
    }

    @Test
    void testOrderWithAbsoluteDiscount() throws Exception {
        CollaborativeOrder order = organizeSampleOrder();
        order = orders.setDiscount(order.getId(), SIMON, Discount.absolute(money(5)));

        final CalculatedPrices totals = order.getCalculatedPrices();
        totals.checkConsistency();
        assertThat(totals.getOriginalPrice()).isEqualTo(money(35));
    }

    @Test
    void testOrderWithRelativeDiscount() throws Exception {
        CollaborativeOrder order = organizeSampleOrder();
        order = orders.setDiscount(order.getId(), SIMON, Discount.relative(percent(0.1)));

        final CalculatedPrices totals = order.getCalculatedPrices();
        totals.checkConsistency();
        assertThat(totals.getOriginalPrice()).isEqualTo(money(35));
    }

    @Test
    void testOrderWithTip() throws Exception {
        CollaborativeOrder order = organizeSampleOrder();
        order = orders.setTip(order.getId(), SIMON, Tip.absolute(money(1.0)));
        order = orders.setTip(order.getId(), SIMON, Tip.relative(percent(0.1)));

        final CalculatedPrices totals = order.getCalculatedPrices();
        totals.checkConsistency();
        assertThat(totals.getOriginalPrice()).isEqualTo(money(35));
    }

    @Test
    void testOrderWithTipAndDiscount() throws Exception {
        CollaborativeOrder order = organizeSampleOrder();
        order = orders.setDiscount(order.getId(), SIMON, Discount.absolute(money(5)));
        order = orders.setTip(order.getId(), SIMON, Tip.absolute(money(1.0)));
        order = orders.setTip(order.getId(), SIMON, Tip.relative(percent(0.1)));

        final CalculatedPrices totals = order.getCalculatedPrices();
        totals.checkConsistency();
        assertThat(totals.getOriginalPrice()).isEqualTo(money(35));
    }

    @Test
    void testUpdateForeignOrder() throws Exception {
        final CollaborativeOrder order = orders.organizeCollaborativeOrder("Test", SIMON);
        assertThatExceptionOfType(Exception.class)
                .isThrownBy(() -> orders.setDiscount(order.getId(), ROBERT, Discount.NONE));
    }

    @Test
    void testJoinOrderClosedForJoining() throws Exception {
        final CollaborativeOrder order = organizeSampleOrder();
        orders.closeOrderForJoining(order.getId(), SIMON);
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> orders.join(order.getId(), TIMO));
    }

    @Test
    void testJoinOrderClosedForModification() throws Exception {
        final CollaborativeOrder order = organizeSampleOrder();
        orders.closeOrderForModification(order.getId(), SIMON);
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> orders.join(order.getId(), TIMO));
    }

    @Test
    void testParticipantSetTipOnOrderClosedForModification() throws Exception {
        final CollaborativeOrder order = organizeSampleOrder();
        orders.closeOrderForModification(order.getId(), SIMON);
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> orders.setTip(order.getId(), SIMON, Tip.absolute(Money.ZERO)));
    }

    @Test
    void testParticipantAddItemToOrderClosedForModification() throws Exception {
        final CollaborativeOrder order = organizeSampleOrder();
        orders.closeOrderForModification(order.getId(), SIMON);
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> orders.addLineItem(order.getId(), SIMON, bread));
    }

    private Money priced(double value) {
        return money(value);
    }
}