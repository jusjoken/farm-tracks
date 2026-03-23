package ca.jusjoken.views;

import java.util.Comparator;
import java.util.List;

import com.flowingcode.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.StockTypeService;
import jakarta.annotation.security.PermitAll;

/**
 * Dashboard – the default landing view for authenticated users.
 *
 * Each stat card is built by {@link #createCard} and displayed in a
 * flex-wrap layout that collapses to a single column on mobile and
 * expands to multiple columns on larger screens.
 *
 * To add a new dashboard card, create a helper method following the
 * existing pattern and add it to the cardGrid in {@link #buildView}.
 */
@PageTitle("Dashboard")
@Route(value = "dashboard", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@RouteAlias(value = "home", layout = MainLayout.class)
@PermitAll
public class DashboardView extends Main {

    private final StockService stockService;
    private final LitterService litterService;
        private final StockTypeService stockTypeService;

    public DashboardView() {
        this.stockService = Registry.getBean(StockService.class);
        this.litterService = Registry.getBean(LitterService.class);
                this.stockTypeService = Registry.getBean(StockTypeService.class);
        buildView();
    }

    // -----------------------------------------------------------------------
    // View construction
    // -----------------------------------------------------------------------

    private void buildView() {
        setSizeFull();
        getStyle().set("overflow", "auto");

        Div root = new Div();
        root.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("box-sizing", "border-box")
                .set("width", "100%");

        List<StockType> stockTypes = stockTypeService.findAllStockTypes().stream()
                .sorted(Comparator.comparing(StockType::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        boolean firstSection = true;
        for (StockType stockType : stockTypes) {
                        if (stockService.countByStockType(stockType) == 0) {
                                continue;
                        }

            if (!firstSection) {
                Div divider = new Div();
                divider.getStyle()
                        .set("height", "1px")
                        .set("background", "var(--lumo-contrast-20pct)")
                        .set("width", "100%");
                root.add(divider);
            }
            firstSection = false;

            String typeTitle = stockType.getName();
            H3 sectionTitle = new H3(typeTitle);
            sectionTitle.getStyle().set("margin", "0");
            root.add(sectionTitle);

            root.add(buildCardGridForStockType(stockType));
        }

        add(root);
    }

    private Div buildCardGridForStockType(StockType stockType) {
                List<Stock> activeBreeders = stockService.findAllBreeders(stockType).stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .toList();

        List<Litter> activeLitters = litterService.getActiveLitters(stockType);
        List<Litter> allLitters = litterService.getAllLitters(stockType);

        long does = activeBreeders.stream().filter(s -> s.getSex() == Utility.Gender.FEMALE).count();
        long bucks = activeBreeders.stream().filter(s -> s.getSex() == Utility.Gender.MALE).count();
        long totalBreeders = activeBreeders.size();

        int activeLitterCount = activeLitters.size();
        int activeKits = activeLitters.stream().mapToInt(l -> safeInt(l.getKitsCount())).sum();

        int totalBorn = allLitters.stream().mapToInt(l -> safeInt(l.getKitsCount())).sum();
        int totalSold = allLitters.stream().mapToInt(l -> safeInt(l.getSoldKitsCount())).sum();
        int totalDied = allLitters.stream().mapToInt(l -> safeInt(l.getDiedKitsCount())).sum();
        int totalLitters = allLitters.size();

        double avgKits = totalLitters > 0 ? (double) totalBorn / totalLitters : 0.0;
        int liveKits = Math.max(0, totalBorn - totalDied);
        double survivalRate = totalBorn > 0 ? (double) liveKits / totalBorn * 100.0 : 0.0;

        Div cardGrid = new Div();
        cardGrid.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-m)")
                .set("align-items", "stretch")
                .set("align-content", "flex-start")
                .set("box-sizing", "border-box")
                .set("width", "100%");

        cardGrid.add(createCard(
                FontAwesome.Solid.PAW.create(),
                "Active Breeders",
                String.valueOf(totalBreeders), "Total",
                statRow("Does", String.valueOf(does)),
                statRow("Bucks", String.valueOf(bucks))
        ));

        cardGrid.add(createCard(
                FontAwesome.Solid.LAYER_GROUP.create(),
                "Active Litters",
                String.valueOf(activeLitterCount), "Litters",
                statRow("Kits in Nest", String.valueOf(activeKits))
        ));

        cardGrid.add(createCard(
                FontAwesome.Solid.STAR.create(),
                "Kits Born",
                String.valueOf(totalBorn), "Kits",
                statRow("Sold", String.valueOf(totalSold)),
                statRow("Died / Culled", String.valueOf(totalDied))
        ));

        cardGrid.add(createCard(
                FontAwesome.Solid.CHART_BAR.create(),
                "Kits / Litters",
                String.format("%.1f", avgKits), "Avg per Litter",
                statRow("Total Kits", String.valueOf(totalBorn)),
                statRow("Total Litters", String.valueOf(totalLitters))
        ));

        cardGrid.add(createCard(
                FontAwesome.Solid.HEART.create(),
                "Kit Survival Rate",
                String.format("%.1f%%", survivalRate), "Survival",
                statRow("Live Kits", String.valueOf(liveKits)),
                statRow("Died", String.valueOf(totalDied))
        ));

        return cardGrid;
    }

    // -----------------------------------------------------------------------
    // Card factory – add new cards by calling createCard() in buildView()
    // -----------------------------------------------------------------------

    /**
     * Builds a stats card.
     *
     * @param icon       large icon shown at the top
     * @param title      card heading
     * @param heroValue  optional big number (null = omit)
     * @param heroLabel  label shown next to heroValue (null = omit)
     * @param statRows   zero or more stat rows (label / value pairs)
     */
    private Card createCard(Component icon, String title,
                            String heroValue, String heroLabel,
                            Div... statRows) {
        Card card = new Card();
        card.addThemeVariants(CardVariant.LUMO_ELEVATED);
        card.getStyle()
                .set("flex", "1 1 260px")
                .set("min-width", "220px")
                .set("max-width", "380px")
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("align-self", "stretch");

        // Icon
        icon.getStyle()
                .set("width", "3em")
                .set("height", "3em")
                .set("color", "var(--lumo-primary-color)");

        Div iconWrap = new Div(icon);
        iconWrap.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("padding-bottom", "var(--lumo-space-xs)");

        // Title
        H3 titleEl = new H3(title);
        titleEl.getStyle()
                .set("margin", "0 0 var(--lumo-space-xs) 0")
                .set("text-align", "center")
                .set("font-size", "var(--lumo-font-size-l)");

        // Hero stat
        Div heroDiv = null;
        if (heroValue != null) {
            Span heroNum = new Span(heroValue);
            heroNum.getStyle()
                    .set("font-size", "var(--lumo-font-size-xxxl)")
                    .set("font-weight", "bold")
                    .set("color", "var(--lumo-primary-color)");

            if (heroLabel != null && !heroLabel.isBlank()) {
                Span heroLbl = new Span(" " + heroLabel);
                heroLbl.getStyle()
                        .set("font-size", "var(--lumo-font-size-s)")
                        .set("color", "var(--lumo-secondary-text-color)");
                heroDiv = new Div(heroNum, heroLbl);
            } else {
                heroDiv = new Div(heroNum);
            }
            heroDiv.getStyle()
                    .set("text-align", "center")
                    .set("padding", "var(--lumo-space-xs) 0");
        }

        // Content wrapper (all vertically centered)
        Div content = new Div();
        content.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("padding", "var(--lumo-space-m)")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("flex", "1 1 auto");

        content.add(iconWrap, titleEl);
        if (heroDiv != null) {
            content.add(heroDiv);
        }

        // Stat rows
        if (statRows.length > 0) {
            Div statsDiv = new Div();
            statsDiv.getStyle()
                    .set("display", "flex")
                    .set("flex-direction", "column")
                    .set("gap", "2px")
                    .set("width", "100%")
                    .set("margin-top", "var(--lumo-space-s)")
                    .set("padding-top", "var(--lumo-space-s)")
                    .set("border-top", "1px solid var(--lumo-contrast-10pct)");

            for (Div row : statRows) {
                statsDiv.add(row);
            }
            content.add(statsDiv);
        }

        card.add(content);
        return card;
    }

    /** A single label / value row used inside a card's stat section. */
    private Div statRow(String label, String value) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-m)");

        Div row = new Div(labelSpan, valueSpan);
        row.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("padding", "2px 0")
                .set("width", "100%")
                .set("gap", "8px");
        return row;
    }

    private static int safeInt(Integer v) {
        return v != null ? v : 0;
    }
}
