package gui;

import static dto.Properties.IMAGE_EXTENSION;
import static gui.GUI.showExceptionError;
import static gui.GUI.showWarning;
import imageAcquisition.ImageProducer;
import imageAqcuisition.imageInputSource.ImageSequence;
import imageProcessing.DownSampler;
import imageProcessing.ImageProcessor;
import imageProcessing.ImageTools;
import imageProcessing.ImageTools.ImageEntry;
import motorControl.MotorControlSimulator;
import java.io.File;
import java.nio.ByteBuffer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class Controller extends VBox {

    private ImageProducer imageProducer;
    private ImageProcessor imageProcessor;
    private InputViewFeed inputViewFeed;
    private DownSampler downSampler;
    private MotorControlSimulator motorControlSimulator;
    public Stage stage;
    private String inputLocation;
    private String outputLocation;
    private String imageLocation;
    private String originalLogLocation;
    private File inputDirectory;
    private File outputDirectory;
    private boolean simulating = false;

    @FXML
    private ChoiceBox inputResSelector;
    @FXML
    private ChoiceBox outputResSelector;
    @FXML
    private ChoiceBox fileExtSelector;
    @FXML
    private ChoiceBox fileExtSelector1;
    @FXML
    private ChoiceBox imageResSelector;
    @FXML
    private ImageView imageView;
    @FXML
    private Button startResizeBtn;
    @FXML
    private Button resetResizeBtn;
    @FXML
    private Button startSimBtn;
    @FXML
    private Button endSimBtn;
    @FXML
    private TextField inputLocBox;
    @FXML
    private TextField outputLocBox;
    @FXML
    private TextField imageSeqLocBox;
    @FXML
    private TextField originalLogLocBox;
    @FXML
    private Accordion accordion;
    @FXML
    private TitledPane resizePane;
    @FXML
    private TextArea statusBox;

    public void updateImageView(ByteBuffer b) {
        Image img = ImageTools.toJavaFXImage(b); //Oh goodness...
        imageView.setImage(img);
    }

    public void updateStatusBox(String str) {
        statusBox.setText(str);
    }

    public void setImageCount(String destination) {
        int count = 0;
        File files = new File(destination + "\\");
        for (File f : files.listFiles()) {
            if (f.isFile() && (f.getName().endsWith(IMAGE_EXTENSION))) {
                count++;
            }
        }
        dto.Properties.imagecount = count;
    }

    @FXML
    protected void browseFileLocation(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        try {
            Button clickedBtn = (Button) event.getSource();
            TextField locationTF = null;
            String clickedBtnId = clickedBtn.getId();
            switch (clickedBtnId) {
                case "inputBrowseBtn":
                    locationTF = inputLocBox;
                    inputLocation = chooser.showDialog(stage).getAbsolutePath();
                    locationTF.setText(inputLocation);
                    break;
                case "outputBrowseBtn":
                    locationTF = outputLocBox;
                    outputLocation = chooser.showDialog(stage).getAbsolutePath();
                    locationTF.setText(outputLocation);
                    break;
                case "imgBrowseBtn":
                    locationTF = imageSeqLocBox;
                    imageLocation = chooser.showDialog(stage).getAbsolutePath();
                    locationTF.setText(imageLocation);
                    break;
                case "logBrowseBtn":
                    locationTF = originalLogLocBox;
                    originalLogLocation = chooser.showDialog(stage).getAbsolutePath();
                    locationTF.setText(originalLogLocation);
                    break;
                default:
                    break;
            }
        } catch (NullPointerException ex) {
        }
    }

    @FXML
    protected void startResize() {
        try {
            String inputResolution = (String) inputResSelector.getSelectionModel().getSelectedItem();
            switch (inputResolution) {
                case "640*480":
                    dto.Properties.IMAGE_HEIGHT = 480;
                    dto.Properties.IMAGE_WIDTH = 640;
                    break;
                case "1280*720":
                    dto.Properties.IMAGE_HEIGHT = 720;
                    dto.Properties.IMAGE_WIDTH = 1280;
                    break;
                case "1280*960":
                    dto.Properties.IMAGE_HEIGHT = 960;
                    dto.Properties.IMAGE_WIDTH = 1280;
                    break;
            }
            String outputRresolution = (String) outputResSelector.getSelectionModel().getSelectedItem();
            switch (outputRresolution) {
                case "640*480":
                    dto.Properties.DS_IMAGE_HEIGHT = 480;
                    dto.Properties.DS_IMAGE_WIDTH = 640;
                    break;
                case "1280*720":
                    dto.Properties.DS_IMAGE_HEIGHT = 720;
                    dto.Properties.DS_IMAGE_WIDTH = 1280;
                    break;
                case "1280*960":
                    dto.Properties.DS_IMAGE_HEIGHT = 960;
                    dto.Properties.DS_IMAGE_WIDTH = 1280;
                    break;
            }
            String fileExtension = (String) fileExtSelector.getSelectionModel().getSelectedItem();
            switch (fileExtension) {
                case ".jpeg":
                    dto.Properties.IMAGE_EXTENSION = ".jpeg";
                    break;
                case ".jpg":
                    dto.Properties.IMAGE_EXTENSION = ".jpg";
                    break;
            }

            if (inputLocation == null || outputLocation == null) {
                showWarning("No input/output location specified", "Please choose an input location and an output location first.");
                return;
            }
            try {
                setImageCount(inputLocation);
                inputDirectory = new File(inputLocation);
                outputDirectory = new File(outputLocation);
                downSampler = new DownSampler(inputDirectory, outputDirectory);
                downSampler.start();
                dto.Properties.resizerun = true;
                startResizeBtn.setDisable(true);
                resetResizeBtn.setDisable(false);
            } catch (Exception ex) {
                showExceptionError(ex, "FileIOException", "Cannot open File path!");
            }
            statusBox.setVisible(true);
            imageView.setVisible(false);
        } catch (NullPointerException e) {
            showExceptionError(e, "NullPointerException", "Please select a resolution first!");
        }
    }

    @FXML
    protected void resetResize() {
        if (downSampler != null) {
            downSampler.stop();
            downSampler = null;
        }
        inputDirectory = null;
        outputDirectory = null;
        startResizeBtn.setDisable(false);
        resetResizeBtn.setDisable(true);
    }

    @FXML
    protected void startSimulation() {
        if (simulating) {
            motorControlSimulator.stop();
        } else {
            if (imageLocation == null) {
                showWarning("No image location specified", "Please choose an image location first.");
                return;
            }
            if (originalLogLocation == null) {
                showWarning("No original log file location specified", "Please choose an original log file first.");
                return;
            }
            try {
                String imageResolution = (String) imageResSelector.getSelectionModel().getSelectedItem();
                switch (imageResolution) {
                    case "640*480":
                        dto.Properties.IMAGE_HEIGHT = 480;
                        dto.Properties.IMAGE_WIDTH = 640;
                        break;
                    case "1280*720":
                        dto.Properties.IMAGE_HEIGHT = 720;
                        dto.Properties.IMAGE_WIDTH = 1280;
                        break;
                    case "1280*960":
                        dto.Properties.IMAGE_HEIGHT = 960;
                        dto.Properties.IMAGE_WIDTH = 1280;
                        break;
                }
                String fileExtension = (String) fileExtSelector1.getSelectionModel().getSelectedItem();
                switch (fileExtension) {
                    case ".jpeg":
                        dto.Properties.IMAGE_EXTENSION = ".jpeg";
                        break;
                    case ".jpg":
                        dto.Properties.IMAGE_EXTENSION = ".jpg";
                        break;
                }

                dto.Properties.SEGMENTATION_WINDOW_SIZE = (int) Math.ceil((double) (dto.Properties.SEGMENTATION_WINDOW_SIZE * dto.Properties.IMAGE_HEIGHT * dto.Properties.IMAGE_WIDTH) / (640 * 480));
                dto.Properties.SEGMENTATION_COMPONENT_MIN_SIZE = (int) Math.ceil((double) (dto.Properties.SEGMENTATION_COMPONENT_MIN_SIZE * dto.Properties.IMAGE_HEIGHT * dto.Properties.IMAGE_WIDTH) / (640 * 480));
                dto.Properties.MOTOR_PX_PER_STEP_X = (double) (dto.Properties.IMAGE_WIDTH * dto.Properties.MOTOR_PX_PER_STEP_X) / 640;
                dto.Properties.MOTOR_PX_PER_STEP_Y = (double) (dto.Properties.IMAGE_HEIGHT * dto.Properties.MOTOR_PX_PER_STEP_Y) / 480;
                dto.Properties.MOVE_DECISION_CONFIDENCE_DISTANCE = (double) (dto.Properties.IMAGE_HEIGHT * dto.Properties.IMAGE_WIDTH * dto.Properties.MOVE_DECISION_CONFIDENCE_DISTANCE) / (640 * 480);
                dto.Properties.MOVE_DECISION_BOUNDARY_PX = (int) (dto.Properties.IMAGE_WIDTH * dto.Properties.MOVE_DECISION_BOUNDARY_RATIO);
            } catch (NullPointerException e) {
                showExceptionError(e, "NullPointerException", "Please select a resolution first!");
            }
            try {
                setImageCount(imageLocation);
                imageProducer = new ImageProducer(new ImageSequence(imageLocation));
                if (imageProducer == null) {
                    showWarning("Input folder unreadable", "Input folder unreadable.");
                    return;
                }
                motorControlSimulator = new MotorControlSimulator();
                if (imageProcessor == null) {
                    imageProcessor = new ImageProcessor(imageProducer, originalLogLocation, imageLocation);
                    motorControlSimulator.attach(imageProcessor);
                }
                motorControlSimulator.start();
                inputViewFeed = new InputViewFeed(imageProducer, this);

                dto.Properties.simrun = true;
                inputViewFeed.start();
                inputViewFeed.attach(imageProcessor);
                imageProducer.start();
                imageProcessor.start();

                imageView.setVisible(true);
                statusBox.setVisible(false);
                startSimBtn.setDisable(true);
                endSimBtn.setDisable(false);
                simulating = true;
            } catch (Exception ex) {
                ex.printStackTrace();
                showExceptionError(ex, "FileIOException", "Cannot open File path!");

            }
        }
    }

    @FXML
    public void endSimulation() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (imageProcessor != null) {
                    imageProcessor.stop();
                    imageProcessor = null;
                }
                if (imageProducer != null) {
                    imageProducer.stop();;
                    imageProducer = null;
                }
                motorControlSimulator.detach();
                motorControlSimulator.stop();
                inputViewFeed.detach();
                inputViewFeed = null;

                dto.Properties.simrun = false;
                simulating = false;
                startSimBtn.setDisable(false);
                endSimBtn.setDisable(true);
            }
        });
    }

    public void initialize() {
        accordion.setExpandedPane(resizePane);
    }

    public static class InputViewFeed implements Runnable {

        ImageProducer src;
        ImageProcessor prc;
        Controller dest;
        Thread thread;

        public InputViewFeed(ImageProducer source, Controller destination) {
            src = source;
            dest = destination;
            thread = new Thread(this, "InputViewFeed");
        }

        public void attach(ImageProcessor processor) {
            prc = processor;
        }

        public void detach() {
            prc = null;
        }

        public void start() {
            thread.start();
        }

        @Override
        public void run() {
            while (dto.Properties.simrun) {
                ImageEntry entry = src.peek();
                if (entry == null) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                ByteBuffer clone = null;
                synchronized (entry) {
                    ByteBuffer img = entry.img;
                    try {
                        clone = ByteBuffer.allocate(img.capacity());
                        img.rewind();
                        clone.put(img);
                        img.rewind();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
                if (clone != null) {
                    clone.flip();
                    if (prc != null) {
                        dest.updateImageView(prc.overlayImage(clone));
                    } else {
                        dest.updateImageView(clone);
                    }
                }
            }
        }
    }

}
