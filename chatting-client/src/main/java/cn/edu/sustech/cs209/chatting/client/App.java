// for (String active : actives) {
//         userSel.getItems().add(new CheckBox(active));
//         }
//
//         List<String> selectedOptions = new ArrayList<>();
//
//         Button okBtn = new Button("OK");
//         okBtn.setOnAction(e -> {
//         for (CheckBox checkBox : userSel.getItems()) {
//         if (checkBox.isSelected()) {
//         selectedOptions.add(checkBox.getText());
//         }
//         }
//         System.out.println("Selected options: " + selectedOptions);
//         stage.close();
//         });