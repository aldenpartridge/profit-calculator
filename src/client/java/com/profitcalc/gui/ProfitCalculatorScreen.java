package com.profitcalc.gui;

import com.profitcalc.calculator.ProfitCalculator;
import com.profitcalc.model.ProfitCalculation;
import com.profitcalc.model.RecipeIngredient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ProfitCalculatorScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget budgetField;
    private List<ProfitCalculation> profitableItems = new ArrayList<>();
    private int scrollOffset = 0;
    private int selectedIndex = -1;

    private static final int ITEM_HEIGHT = 20;
    private static final int LIST_WIDTH = 250;
    private static final int DETAIL_WIDTH = 250;

    public ProfitCalculatorScreen(Screen parent) {
        super(Text.literal("Profit Calculator"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        // Budget input field
        budgetField = new TextFieldWidget(
            this.textRenderer,
            this.width / 2 - 100,
            20,
            200,
            20,
            Text.literal("Budget")
        );
        budgetField.setMaxLength(10);
        budgetField.setText("1000");
        budgetField.setPlaceholder(Text.literal("Enter budget..."));
        this.addDrawableChild(budgetField);

        // Calculate button
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Calculate Profits"),
            button -> calculateProfits()
        ).dimensions(this.width / 2 - 100, 45, 200, 20).build());

        // Close button
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Close"),
            button -> this.close()
        ).dimensions(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    private void calculateProfits() {
        try {
            double budget = Double.parseDouble(budgetField.getText());
            profitableItems = ProfitCalculator.getInstance().findProfitableItems(budget);
            scrollOffset = 0;
            selectedIndex = -1;
        } catch (NumberFormatException e) {
            profitableItems = new ArrayList<>();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Draw title
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            this.title,
            this.width / 2,
            5,
            0xFFFFFF
        );

        // Draw budget label
        context.drawTextWithShadow(
            this.textRenderer,
            "Budget:",
            this.width / 2 - 100,
            10,
            0xFFFFFF
        );

        // Draw results
        if (!profitableItems.isEmpty()) {
            drawItemList(context, mouseX, mouseY);
            drawItemDetails(context);
        } else {
            context.drawCenteredTextWithShadow(
                this.textRenderer,
                "No profitable items found. Browse /ah to collect price data.",
                this.width / 2,
                this.height / 2,
                0xAAAAAA
            );
        }
    }

    private void drawItemList(DrawContext context, int mouseX, int mouseY) {
        int listX = 10;
        int listY = 75;
        int listHeight = this.height - 120;

        // Draw background
        context.fill(listX, listY, listX + LIST_WIDTH, listY + listHeight, 0x80000000);

        // Draw header
        context.drawTextWithShadow(
            this.textRenderer,
            "Profitable Items (sorted by margin)",
            listX + 5,
            listY - 12,
            0xFFFFFF
        );

        // Draw items
        int visibleItems = listHeight / ITEM_HEIGHT;
        int maxScroll = Math.max(0, profitableItems.size() - visibleItems);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        for (int i = 0; i < visibleItems && (i + scrollOffset) < profitableItems.size(); i++) {
            int index = i + scrollOffset;
            ProfitCalculation item = profitableItems.get(index);

            int itemY = listY + i * ITEM_HEIGHT;
            boolean hovered = mouseX >= listX && mouseX <= listX + LIST_WIDTH &&
                            mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT;
            boolean selected = index == selectedIndex;

            // Draw selection/hover background
            if (selected) {
                context.fill(listX, itemY, listX + LIST_WIDTH, itemY + ITEM_HEIGHT, 0x80FFFFFF);
            } else if (hovered) {
                context.fill(listX, itemY, listX + LIST_WIDTH, itemY + ITEM_HEIGHT, 0x40FFFFFF);
            }

            // Draw item name and profit
            String itemText = item.getItem().getName().getString();
            String profitText = String.format("+$%.2f (%.0f%%)",
                item.getProfit(), item.getProfitMargin());

            context.drawTextWithShadow(
                this.textRenderer,
                itemText,
                listX + 5,
                itemY + 6,
                selected ? 0x000000 : 0xFFFFFF
            );

            context.drawTextWithShadow(
                this.textRenderer,
                profitText,
                listX + LIST_WIDTH - this.textRenderer.getWidth(profitText) - 5,
                itemY + 6,
                selected ? 0x00AA00 : 0x00FF00
            );
        }
    }

    private void drawItemDetails(DrawContext context) {
        if (selectedIndex < 0 || selectedIndex >= profitableItems.size()) {
            return;
        }

        ProfitCalculation item = profitableItems.get(selectedIndex);

        int detailX = this.width - DETAIL_WIDTH - 10;
        int detailY = 75;
        int detailHeight = this.height - 120;

        // Draw background
        context.fill(detailX, detailY, detailX + DETAIL_WIDTH, detailY + detailHeight, 0x80000000);

        int y = detailY + 5;
        int lineHeight = 12;

        // Item name
        context.drawTextWithShadow(
            this.textRenderer,
            item.getItem().getName().getString(),
            detailX + 5,
            y,
            0xFFFF00
        );
        y += lineHeight + 5;

        // Selling price
        context.drawTextWithShadow(
            this.textRenderer,
            String.format("Selling: $%.2f", item.getSellingPrice()),
            detailX + 5,
            y,
            0xFFFFFF
        );
        y += lineHeight;

        // Materials cost
        context.drawTextWithShadow(
            this.textRenderer,
            String.format("Cost: $%.2f", item.getMaterialsCost()),
            detailX + 5,
            y,
            0xFFFFFF
        );
        y += lineHeight;

        // Profit
        context.drawTextWithShadow(
            this.textRenderer,
            String.format("Profit: $%.2f (%.1f%%)",
                item.getProfit(), item.getProfitMargin()),
            detailX + 5,
            y,
            0x00FF00
        );
        y += lineHeight + 10;

        // Recipe header
        context.drawTextWithShadow(
            this.textRenderer,
            "Materials needed:",
            detailX + 5,
            y,
            0xAAAAFF
        );
        y += lineHeight;

        // Recipe ingredients
        for (RecipeIngredient ingredient : item.getRecipe().getIngredients()) {
            Double price = item.getMaterialPrices().get(ingredient.getItem());
            String text = String.format("%dx %s @ $%.2f",
                ingredient.getQuantity(),
                ingredient.getItem().getName().getString(),
                price != null ? price : 0);

            context.drawTextWithShadow(
                this.textRenderer,
                text,
                detailX + 10,
                y,
                0xCCCCCC
            );
            y += lineHeight;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Check if clicked on item list
        int listX = 10;
        int listY = 75;
        int listHeight = this.height - 120;

        if (mouseX >= listX && mouseX <= listX + LIST_WIDTH &&
            mouseY >= listY && mouseY <= listY + listHeight) {

            int clickedIndex = ((int) mouseY - listY) / ITEM_HEIGHT + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < profitableItems.size()) {
                selectedIndex = clickedIndex;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (verticalAmount < 0) {
            scrollOffset++;
        }
        return true;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
