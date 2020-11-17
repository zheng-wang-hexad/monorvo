package film.monovo.gui.event;

import film.monovo.manager.EventManager;
import film.monovo.manager.FileManager;
import film.monovo.manager.event.Event;
import film.monovo.manager.event.EventChain;
import film.monovo.manager.event.EventContent;
import film.monovo.manager.event.EventType;
import film.monovo.manager.order.Order;
import film.monovo.manager.order.OrderStatus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.*;
import java.util.stream.Collectors;

public class ImportDialog extends Dialog<HashMap<String, Boolean>> {
    private final HashSet<Order> suggested = new HashSet<>();
    public final ObservableList<String> orderIds = FXCollections.observableArrayList();

    private ListView<String> suggestion = new ListView<>(orderIds);
    private TextField input = new TextField();

    protected HashMap<String, Boolean> output = new HashMap<>();
    protected Optional<String> orderId = Optional.empty();
    private Button importOrder = new Button("import");


    public ImportDialog(EventChain chain, EventManager manager) {
        getSuggestedOrders(chain, manager);
        setupInput(manager);
        var box = new VBox();
        var selectBox = new HBox();
        selectBox.getChildren().add(new Label("ID:"));
        selectBox.setSpacing(5);
        selectBox.getChildren().add(input);
        box.getChildren().add(selectBox);
        box.getChildren().add(suggestion);
        setupButtons(chain, manager);
//        box.getChildren().add(setupButtons(chain, manager));
        for (Order o : suggested) {
            this.orderIds.add(o.id);
        }
        this.getDialogPane().setContent(box);

    }

    private void setupInput(EventManager manager) {
        suggestion.setOnMouseClicked(action -> {
            input.setText(suggestion.getSelectionModel().getSelectedItem());
        });

        input.textProperty().addListener((observable, oldValue, newValue) -> {
            orderIds.clear();
            if(input.getText().isBlank()) {
                for(Order o : suggested) {
                    orderIds.add(o.id);
                }
            } else {
                this.suggestion.getItems().clear();
                var list = manager.managers.orderManager.getOrders().stream().filter(it -> {return it.id.toLowerCase().contains(newValue.toLowerCase());})
                        .filter( it-> !this.suggested.contains(it))
                        .collect(Collectors.toList());
                for(Order o : list) {
                    orderIds.add(o.id);
                }

                this.importOrder.setDisable(list.stream().noneMatch(it -> it.id.equals(newValue)));
            }
        });
    }

    private void setupButtons(EventChain chain, EventManager manager) {
//        var buttons = new HBox();
//        var cancel = new Button("cancel");
//        cancel.setOnAction(a -> {
//            this.orderId = Optional.empty();
//            this.output = Optional.empty();
//            this.close();
//        });
//        importOrder.setOnAction(a -> {
//            this.orderId = Optional.of(input.getText());
//            importToOrder(chain, manager);
//            this.close();
//        });
//
//        buttons.getChildren().add(cancel);
//        buttons.getChildren().add(importOrder);
//        return buttons;

        ButtonType buttonTypeOk = new ButtonType("Okay", ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().add(buttonTypeOk);
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        this.getDialogPane().getButtonTypes().add(buttonTypeCancel);

        this.setResultConverter(new Callback<ButtonType, HashMap<String, Boolean>>() {
            @Override
            public HashMap<String, Boolean> call(ButtonType b) {
                if (b == buttonTypeOk) {
                    if(manager.managers.orderManager.hasOrder(input.getText())) {

                        orderId = Optional.of(input.getText());
                        importToOrder(chain, manager);
                        return output;
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setContentText("order is not found");
                        alert.showAndWait();
                        return null;
                    }
                }else {
                    orderId = Optional.empty();
                    return null;
                }
            }
        });

    }

    private void importToOrder(EventChain chain, EventManager manager) {
        var result = new HashMap<String, Boolean>();
        for( Order o : manager.managers.orderManager.getOrders()){
            if(o.id.equals(this.input.getText())) {
                for(String str: FileManager.readAllImageFilePath(o.id)) {
                    result.put(str, true);
                }
            }
        }

        for(Event e: chain.events) {
            var imported = result.values();
            for(EventContent c: e.contents) {
                if(c.type == EventType.IMAGE && !c.content.isBlank() && !imported.contains(c.content)) {
                    result.put(c.content, false);
                }
            }
        }

        this.output = result;
    }

    private void getSuggestedOrders(EventChain chain, EventManager manager) {
        var orders = manager.managers.orderManager.getOrders();
        for(Order o : orders) {
            if(textContainsID(chain, o)) {
                suggested.clear();
                suggested.add(o);
                return;
            }

            if(chain.event.from.contains(o.customer.email) && o.status != OrderStatus.CLOSED && o.status != OrderStatus.DELIVERED) {
                suggested.add(o);
            }
        }
    }

    private boolean textContainsID(EventChain chain, Order order) {
        for(Event e : chain.events) {
            if(e.subject.contains(order.id)){
                return true;
            }

            for(EventContent ec: e.contents) {
                if(ec.type == EventType.TXT && ec.content.toLowerCase().contains(order.id.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }


}