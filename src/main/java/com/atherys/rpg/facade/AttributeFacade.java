package com.atherys.rpg.facade;

import com.atherys.rpg.api.stat.AttributeType;
import com.atherys.rpg.character.PlayerCharacter;
import com.atherys.rpg.command.exception.RPGCommandException;
import com.atherys.rpg.config.AtherysRPGConfig;
import com.atherys.rpg.data.AttributeData;
import com.atherys.rpg.service.AttributeService;
import com.atherys.rpg.service.ExpressionService;
import com.atherys.rpg.service.RPGCharacterService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Singleton
public class AttributeFacade {

    @Inject
    private AtherysRPGConfig config;

    @Inject
    private RPGCharacterService characterService;

    @Inject
    private RPGMessagingFacade rpgMsg;

    @Inject
    private AttributeService attributeService;

    @Inject
    private ExpressionService expressionService;

    public void addPlayerAttribute(Player player, AttributeType attributeType, double amount) throws RPGCommandException {
        PlayerCharacter pc = characterService.getOrCreateCharacter(player);

        if (validateAttribute(pc.getBaseAttributes().getOrDefault(attributeType, config.ATTRIBUTE_MIN) + amount)) {
            characterService.addAttribute(pc, attributeType, amount);
        }
    }

    public void removePlayerAttribute(Player player, AttributeType attributeType, double amount) throws RPGCommandException {
        PlayerCharacter pc = characterService.getOrCreateCharacter(player);

        if (validateAttribute(pc.getBaseAttributes().getOrDefault(attributeType, config.ATTRIBUTE_MIN) - amount)) {
            characterService.removeAttribute(pc, attributeType, amount);
        }
    }

    private boolean validateAttribute(double amount) throws RPGCommandException {
        if (amount < config.ATTRIBUTE_MIN) {
            throw new RPGCommandException("A player cannot have attributes less than ", config.ATTRIBUTE_MIN);
        }

        if (amount > config.ATTRIBUTE_MAX) {
            throw new RPGCommandException("A player cannot have attributes bigger than ", config.ATTRIBUTE_MAX);
        }

        return true;
    }

    public void purchaseAttribute(Player player, AttributeType type, double amount) {
        PlayerCharacter pc = characterService.getOrCreateCharacter(player);

        double expCost = amount * config.ATTRIBUTE_UPGRADE_COST;

        // If the player has already reached their experience spending limit, cancel
        if (pc.getSpentExperience() + expCost > pc.getExperienceSpendingLimit()) {
            rpgMsg.error(player, "You cannot go over your experience spending limit of ", pc.getExperienceSpendingLimit());
        } else {

            if (pc.getExperience() - expCost < config.EXPERIENCE_MIN) {
                rpgMsg.error(player, "You do not have enough experience to increase this attribute.");
                return;
            }

            double afterPurchase = pc.getBaseAttributes().getOrDefault(type, config.ATTRIBUTE_MIN) + amount;

            if (afterPurchase > config.ATTRIBUTE_MAX) {
                rpgMsg.error(player, "You cannot have more than a base of ", config.ATTRIBUTE_MAX, " in this attribute.");
                return;
            }

            characterService.addAttribute(pc, type, amount);
            characterService.removeExperience(pc, expCost);
            characterService.addSpentExperience(pc, expCost);

            rpgMsg.info(player,
                    "You have added ", 1.0, " ",
                    type.getColor(), type.getName(), TextColors.RESET,
                    " for ", TextColors.GOLD, config.ATTRIBUTE_UPGRADE_COST, TextColors.RESET,
                    " experience."
            );
        }
    }

    public void enchantPlayerHeldItem(Player source, AttributeType attributeType, Double amount) throws RPGCommandException {
        ItemStack item = source.getEquipped(EquipmentTypes.MAIN_HAND).orElse(null);

        if (item == null) {
            throw new RPGCommandException("You must be holding an item in order to enchant it with an attribute.");
        }

        setItemAttributeValue(item, attributeType, amount);
        updateItemLore(item);
    }

    public void setItemAttributeValue(ItemStack item, AttributeType attributeType, Double amount) throws RPGCommandException {
        AttributeData data = item.get(AttributeData.class).orElseGet(AttributeData::new);
        data.setAttribute(attributeType, amount);
        if (!item.offer(data).isSuccessful()) {
            throw new RPGCommandException("Failed to set data on item.");
        }
    }

    public Map<AttributeType, Double> getAllAttributes(Entity entity) {
        return attributeService.getAllAttributes(entity);
    }

    public Map<AttributeType, Double> getBaseAttributes(Entity entity) {
        return attributeService.getBaseAttributes(characterService.getOrCreateCharacter(entity));
    }

    public Map<AttributeType, Double> getEquipmentAttributes(Entity entity) {
        return attributeService.getEquipmentAttributes(entity);
    }

    public void showPlayerAttributes(Player player) {
        PlayerCharacter pc = characterService.getOrCreateCharacter(player);

        Text.Builder attributeText = Text.builder();

        Map<AttributeType, Double> baseAttributes = pc.getBaseAttributes();
        Map<AttributeType, Double> buffAttributes = pc.getBuffAttributes();
        Map<AttributeType, Double> itemAttributes = attributeService.getEquipmentAttributes(player);

        baseAttributes.forEach((type, value) -> {

            double base = value;
            double buff = buffAttributes.getOrDefault(type, 0.0d);
            double item = itemAttributes.getOrDefault(type, 0.0d);

            double total = base + buff + item;

            Text baseAttribute = Text.builder()
                    .onHover(TextActions.showText(Text.of("Base")))
                    .append(Text.of(TextColors.RED, base, TextColors.RESET))
                    .build();

            Text buffAttribute = Text.builder()
                    .onHover(TextActions.showText(Text.of("From Buffs")))
                    .append(Text.of(TextColors.GREEN, buff, TextColors.RESET))
                    .build();

            Text itemAttribute = Text.builder()
                    .onHover(TextActions.showText(Text.of("From Equipment")))
                    .append(Text.of(TextColors.BLUE, item, TextColors.RESET))
                    .build();

            Text upgradeButton;

            if (type.isUpgradable()) {
                upgradeButton = getAddAttributeButton(pc, type, value);
            } else {
                upgradeButton = Text.of("[   ]");
            }

            Text attribute = Text.builder()
                    .append(upgradeButton)
                    .append(Text.of(" "))
                    .append(Text.of(type.getColor(), type.getName(), ": ", TextColors.RESET, total, " ( ", baseAttribute , " + ", itemAttribute, " + ", buffAttribute, " )"))
                    .append(Text.NEW_LINE)
                    .build();

            attributeText.append(attribute);
        });

        player.sendMessage(attributeText.build());
    }

    /**
     * Updates the lore of an item to reflect the attributes contained within it
     *
     * @param stack The itemstack whose lore is to be updated
     */
    public void updateItemLore(ItemStack stack) {
        Optional<AttributeData> attributeData = stack.get(AttributeData.class);

        attributeData.ifPresent((data) -> {
            List<Text> lore = stack.get(Keys.ITEM_LORE).orElse(new ArrayList<>());

            data.asMap().forEach((type, value) -> {
                if (value == 0.0d) {
                    return;
                }

                for (int i = 0; i < lore.size(); i++) {
                    // TODO: Remove lore line if attribute is no longer present
                    // TODO: Update amount of attribute if already present
                    if (lore.get(i).toPlain().contains(type.getName())) {
                        lore.set(i, getAttributeLoreLine(type, value));
                        return;
                    }
                }

                lore.add(getAttributeLoreLine(type, value));
            });

            stack.offer(Keys.ITEM_LORE, lore);
        });
    }

    private Text getAddAttributeButton(PlayerCharacter pc, AttributeType type, double currentValue) {

        Consumer<CommandSource> onClick = source -> {
            if (source instanceof Player) {
                purchaseAttribute((Player) source, type, 1.0);
            }
        };

        Text hoverText = Text.builder()
                .append(Text.of("Click to add 1 ", type.getColor(), type.getName(), TextColors.RESET, " for ", config.ATTRIBUTE_UPGRADE_COST, " experience.", Text.NEW_LINE))
                .append(getEffectsOfIncreasingAttribute(pc, type, currentValue))
                .build();

        return Text.builder()
                .append(Text.of(TextColors.RESET, "[ ", TextColors.DARK_GREEN, "+", TextColors.RESET, " ]"))
                .onHover(TextActions.showText(hoverText))
                .onClick(TextActions.executeCallback(onClick))
                .build();
    }

    private Text getEffectsOfIncreasingAttribute(PlayerCharacter pc, AttributeType type, double currentValue) {
        return Text.of("<Placeholder for effects>");
    }

    private double getFinalAttributeValue(PlayerCharacter pc, Player player, AttributeType type) {
        double finalValue = pc.getBaseAttributes().get(type);

        finalValue += attributeService.getHeldItemAttributes(player).getOrDefault(type, 0.0);
        finalValue += attributeService.getArmorAttributes(player).getOrDefault(type, 0.0);
        finalValue += attributeService.getBuffAttributes(pc).getOrDefault(type, 0.0);

        return finalValue;
    }

    private Text getAttributeLoreLine(AttributeType type, Double amount) {
        return Text.of(type.getColor(), type.getName(), ": ", TextColors.WHITE, amount, TextColors.RESET);
    }
}
