package com.gui;

import com.osm.FileScanner;
import com.osm.FileTreeNode;
import com.osm.OSResource;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;

import javafx.scene.control.TreeTableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;


import java.io.File;
import java.util.Dictionary;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class OSMonitorController {
    @FXML private LineChart cpuChart;
    @FXML private TreeTableView<FileTreeNode> fileStat;

    @FXML private Text osType;
    @FXML private Text cpuArch;
    @FXML private Text cpuCore;

    //定时器任务
    private TimerTask timerTask = null;
    //定时器线程执行定时任务
    private Timer timer = new Timer();

    private Stage primaryStage = null;

   private final Image image = new Image(getClass().getClassLoader().getResourceAsStream("Folder.png"));


    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void handleCPUSelectionChanged(Event event) {

        Tab tab = (Tab)event.getTarget();

        if (tab.isSelected()) {
            //匿名内部类
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    OSResource.XYPair[] xyPairs = OSResource.getCPUPercetage();
                    XYChart.Series series = new XYChart.Series();
                    for (OSResource.XYPair xyPair : xyPairs) {
                        XYChart.Data data = new XYChart.Data(xyPair.getX(),xyPair.getY());
                        series.getData().add(data);
                    }
                    //将渲染逻辑切换到主线程运行
                    Platform.runLater( () ->{
                            if (cpuChart.getData().size() > 0) {
                                cpuChart.getData().remove(0);
                            }
                            cpuChart.getData().add(series);

                            osType.setText(OSResource.getOSName());
                            cpuArch.setText(OSResource.getCUPArch());
                            cpuCore.setText(OSResource.getCPUCores());
                        }
                    );
                }
            };
            //第二非参数 0 ，表示任务安排好以后，立即执行一次
            //第三个参数表示执行周期时间，单位是 1000ms
            timer.schedule(timerTask,0,1000);

        }else {
            if (timerTask != null) {
                timerTask.cancel();
                timerTask = null;
            }
        }
    }

    public void shutDown() {
        if (timer != null) {
            timer.cancel();
        }
    }

    public void handleSelectFile(ActionEvent actionEvent) {
        System.out.println("Button action");

        fileStat.setRoot(null);

        DirectoryChooser dictionaryChooser = new DirectoryChooser();
        File file = dictionaryChooser.showDialog(primaryStage);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                FileTreeNode rootNode = new FileTreeNode();
                rootNode.setFile(file);
                rootNode.setFileName(file.getName());

                FileScanner.scannerDirectory(rootNode);

                TreeItem rootItem = new TreeItem(rootNode,new ImageView(image));
                rootItem.setExpanded(true);
                fillTreeItem(rootNode,rootItem);

                //转到主线程中进行
                Platform.runLater(
                        () ->{
                            fileStat.setRoot(rootItem);
                        }
                );
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void fillTreeItem(FileTreeNode rootNode,TreeItem rootItem) {
        List<FileTreeNode> childs = rootNode.getChildrens();
        for (FileTreeNode node : childs) {
            TreeItem item = new TreeItem(node);

            if (node.getChildrens().size() > 0) {
                item.setGraphic(new ImageView(image));
            }
            rootItem.getChildren().add(item);

            fillTreeItem(node,item);
        }
    }
}
