package org.psd.parkingspacedetector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;
import org.springframework.stereotype.Service;
import org.psd.parkingspacedetector.configuration.MediaConfig;
import org.psd.parkingspacedetector.dto.request.ParkingSlotDetail;
import org.psd.parkingspacedetector.dto.request.ParkingTrainingRequest;
import org.psd.parkingspacedetector.dto.request.PointInfo;
import org.psd.parkingspacedetector.entity.CoordinatesInfo;
import org.psd.parkingspacedetector.entity.Media;
import org.psd.parkingspacedetector.entity.ParkingSlotInfo;
import org.psd.parkingspacedetector.enums.EnumMediaType;
import org.psd.parkingspacedetector.repository.ParkingSlotInfoRepository;
import weka.classifiers.functions.SMO;
import weka.core.*;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingService {

    private final MediaConfig mediaConfig;
    private final MediaService mediaService;
    private final ParkingSlotInfoRepository parkingSlotInfoRepository;

    public File generateModelFile(ParkingTrainingRequest request, String mediaOriginalPath) {
        Long mediaId = request.getMediaId();
        disablePrevTrainModelIfExists(mediaId);

        Mat image = loadImage(mediaOriginalPath);
        List<Attribute> attributes = createAttributes();
        Instances dataset = initializeInstances(attributes);

        for (ParkingSlotDetail slotDetail : request.getSlotDetails()) {
            List<Point> points = extractPoints(slotDetail);
            Rect rectCrop = createRect(points);
            Mat croppedImage = new Mat(image, rectCrop);
            processSlotDetail(slotDetail, croppedImage, dataset);
        }

        return generateModel(dataset, mediaId);
    }

    private File generateModel(Instances dataset, Long mediaId) {
        try {
            dataset.setClassIndex(dataset.numAttributes() - 1);

            SMO svmReg = new SMO();
            svmReg.buildClassifier(dataset);

            String modelFilePath = mediaConfig.getUploadBaseDir() + File.separator + "ParkingSlotModel_" + mediaId + ".model";
            SerializationHelper.write(modelFilePath, svmReg);

            return new File(modelFilePath);
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    private void disablePrevTrainModelIfExists(Long refId) {
        List<Media> mediaByRefId = mediaService.getMediaByRefIdAndMediaTypeId(refId, EnumMediaType.MODEL.getId());
        mediaByRefId.forEach(media -> {
            try {
                mediaService.disableMedia(media.getId());
            } catch (Exception e) {
                log.error("Failed to disable media with ID: " + media.getId(), e);
            }
        });
    }

    private Mat loadImage(String mediaOriginalPath) {
        Mat image = Imgcodecs.imread(mediaOriginalPath);
        if (image.empty()) {
            throw new RuntimeException("Failed to load image.");
        }
        return getNormalizedImage(image);
    }

    private static Mat getNormalizedImage(Mat image) {
        Mat grayImage = new Mat();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);

        Mat blurredImage = new Mat();
        Imgproc.GaussianBlur(grayImage, blurredImage, new org.opencv.core.Size(3, 3), 1);

        Mat thresholdImage = new Mat();
        Imgproc.adaptiveThreshold(
                blurredImage,
                thresholdImage,
                255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                25,
                16
        );

        Mat medianBlurredImage = new Mat();
        Imgproc.medianBlur(thresholdImage, medianBlurredImage, 5);
        return medianBlurredImage;
    }

    private List<Attribute> createAttributes() {
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("NW"));
        attributes.add(new Attribute("N"));
        attributes.add(new Attribute("NE"));
        attributes.add(new Attribute("W"));
        attributes.add(new Attribute("C"));
        attributes.add(new Attribute("E"));
        attributes.add(new Attribute("SW"));
        attributes.add(new Attribute("S"));
        attributes.add(new Attribute("SE"));
        attributes.add(new Attribute("isEmpty", Arrays.asList("true", "false")));
        return attributes;
    }

    private Instances initializeInstances(List<Attribute> attributes) {
        Instances data = new Instances("ParkingSlotTrain", new ArrayList<>(attributes), 0);
        data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

    private void processSlotDetail(ParkingSlotDetail slotDetail, Mat croppedImage, Instances data) {
        double[][] matrix = convertImageToMatrix(croppedImage);
        String[] principalComponents = performPCA(matrix);

        addInstanceToData(data, principalComponents, slotDetail);
    }

    private double[][] convertImageToMatrix(Mat croppedImage) {
        int rows = roundToNearestMultipleOfThree(croppedImage.rows());
        int cols = roundToNearestMultipleOfThree(croppedImage.cols());

        double[][] matrix = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = croppedImage.get(i, j)[0];
            }
        }

        return matrix;
    }

    private void addInstanceToData(Instances data, String[] principalComponents, ParkingSlotDetail slotDetail) {
        int numAttributes = data.numAttributes();
        Instance instance = new DenseInstance(numAttributes);
        instance.setDataset(data);

        for (int i = 0; i < principalComponents.length; i++) {
            if (i < numAttributes - 1) {
                instance.setValue(data.attribute(i), Double.parseDouble(principalComponents[i]));
            }
        }

        if (slotDetail.getIsEmpty() != null) {
            String isEmptyValue = Boolean.TRUE.equals(slotDetail.getIsEmpty()) ? "true" : "false";
            instance.setValue(data.attribute(numAttributes - 1), isEmptyValue);
        }

        data.add(instance);
    }

    private List<Point> extractPoints(ParkingSlotDetail slotDetail) {
        List<Point> points = new ArrayList<>();
        for (PointInfo point : slotDetail.getPoints()) {
            points.add(new Point(point.getRealX().intValue(), point.getRealY().intValue()));
        }
        return points;
    }

    private String[] performPCA(double[][] mainMatrix) {
        int numSubMatrices = 3 * 3;
        String[] principalComponents = new String[numSubMatrices];

        int rows = mainMatrix.length;
        int cols = mainMatrix[0].length;

        int subRows = rows / 3;
        int subCols = cols / 3;

        int index = -1;
        for (int mainRow = 0; mainRow < rows; mainRow += subRows) {
            for (int mainCol = 0; mainCol < cols; mainCol += subCols) {
                index++;
                double[][] subMatrixData = new double[subRows][subCols];
                for (int r = 0; r < subRows && mainRow + r < rows; r++) {
                    for (int c = 0; c < subCols && mainCol + c < cols; c++) {
                        subMatrixData[r][c] = mainMatrix[mainRow + r][mainCol + c];
                    }
                }
                if (index >= numSubMatrices) {
                    log.error("wrong index : " + index);
                } else {
                    principalComponents[index] = extractPrincipalComponent(subMatrixData);
                }
            }
        }
        return principalComponents;
    }

    private String extractPrincipalComponent(double[][] data) {
        RealMatrix matrix = MatrixUtils.createRealMatrix(data);

        centralizeMatrix(matrix);

        SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
        RealMatrix uMatrix = svd.getU();

        double meanValue = calculateMean(uMatrix);
        return formatMean(meanValue);
    }

    private String formatMean(double mean) {
        DecimalFormat df = new DecimalFormat("#.#######");
        return df.format(mean);
    }

    private void centralizeMatrix(RealMatrix matrix) {
        int columnDimension = matrix.getColumnDimension();
        int rowDimension = matrix.getRowDimension();

        double[] columnMeans = new double[columnDimension];
        for (int i = 0; i < columnDimension; i++) {
            columnMeans[i] = matrix.getColumnVector(i).getL1Norm() / rowDimension;
        }

        for (int i = 0; i < rowDimension; i++) {
            for (int j = 0; j < columnDimension; j++) {
                matrix.setEntry(i, j, matrix.getEntry(i, j) - columnMeans[j]);
            }
        }
    }

    private double calculateMean(RealMatrix uMatrix) {
        double sum = 0.0;
        int uRows = uMatrix.getRowDimension();
        int uCols = uMatrix.getColumnDimension();

        for (int i = 0; i < uRows; i++) {
            for (int j = 0; j < uCols; j++) {
                sum += uMatrix.getEntry(i, j);
            }
        }

        return sum / (uRows * uCols);
    }

    private Rect createRect(List<Point> points) {
        int startX = (int) Math.min(Math.min(points.get(0).x, points.get(1).x), Math.min(points.get(2).x, points.get(3).x));
        int startY = (int) Math.min(Math.min(points.get(0).y, points.get(1).y), Math.min(points.get(2).y, points.get(3).y));
        int endX = (int) Math.max(Math.max(points.get(0).x, points.get(1).x), Math.max(points.get(2).x, points.get(3).x));
        int endY = (int) Math.max(Math.max(points.get(0).y, points.get(1).y), Math.max(points.get(2).y, points.get(3).y));

        return new Rect(startX, startY, endX - startX, endY - startY);
    }

    public static int roundToNearestMultipleOfThree(int num) {
        return (num / 3) * 3;
    }

    public File getParkingSlotDetectorResult(Long mediaId, Long modelId) {
        Media testMedia = fetchMedia(mediaId, "Test media not found");
        Media model = fetchMedia(modelId, "Related model not found");

        if (!Objects.equals(testMedia.getRefId(), model.getRefId())) {
            throw new RuntimeException("Model and test media file are not related.");
        }

        Long modelRefId = model.getRefId();
        List<ParkingSlotInfo> parkingSlotInfoList = fetchParkingSlotInfo(modelRefId);

        SMO svmModel = loadSvmModel(model.getOriginalPath());
        VideoCapture video = openVideoFile(testMedia.getOriginalPath());

        String outputVideoPath = mediaConfig.getUploadBaseDir() + File.separator + "processed_video_" + modelRefId + ".mp4";
        VideoWriter videoWriter = initializeVideoWriter(outputVideoPath, video);

        processVideoFrames(video, videoWriter, parkingSlotInfoList, svmModel);

        video.release();
        videoWriter.release();
        return new File(outputVideoPath);
    }

    private Media fetchMedia(Long mediaId, String errorMessage) {
        return mediaService.getMediaById(mediaId)
                .orElseThrow(() -> new RuntimeException(errorMessage));
    }


    private List<ParkingSlotInfo> fetchParkingSlotInfo(Long mediaId) {
        List<ParkingSlotInfo> parkingSlotInfoList = parkingSlotInfoRepository.findAllByMedia_Id(mediaId);
        if (parkingSlotInfoList.isEmpty()) {
            throw new RuntimeException("Slot info not found.");
        }
        return parkingSlotInfoList;
    }

    private SMO loadSvmModel(String modelPath) {
        try {
            return (SMO) SerializationHelper.read(modelPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load model.", e);
        }
    }

    private VideoCapture openVideoFile(String videoPath) {
        VideoCapture video = new VideoCapture(videoPath);
        if (!video.isOpened()) {
            throw new RuntimeException("Failed to open video file.");
        }
        return video;
    }

    private VideoWriter initializeVideoWriter(String outputVideoPath, VideoCapture video) {
        int frameWidth = (int) video.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int frameHeight = (int) video.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        double fps = video.get(Videoio.CAP_PROP_FPS);

        VideoWriter videoWriter = new VideoWriter(outputVideoPath, VideoWriter.fourcc('H', '2', '6', '4'), fps, new Size(frameWidth, frameHeight));

        if (!videoWriter.isOpened()) {
            throw new RuntimeException("Failed to create video writer.");
        }

        return videoWriter;
    }

    private void processVideoFrames(VideoCapture video, VideoWriter videoWriter, List<ParkingSlotInfo> parkingSlotInfoList, SMO svmModel) {
        Mat frame = new Mat();
        int frameCounter = 0;
        List<Double> lastPredictions = new ArrayList<>();
        List<Rect> lastRects = new ArrayList<>();
        double fps = video.get(Videoio.CAP_PROP_FPS);

        while (video.read(frame)) {
            boolean processFrame = (frameCounter % (int) (fps * 1.5) == 0);
            frameCounter++;

            if (processFrame) {
                lastRects.clear();
                lastPredictions.clear();

                List<Attribute> attributes = createAttributes();
                Instances dataset = initializeInstances(attributes);
                Mat normalizedImage = getNormalizedImage(frame);

                for (ParkingSlotInfo slotInfo : parkingSlotInfoList) {
                    ParkingSlotDetail parkingSlotDetail = mapToParkingSlotDetail(slotInfo);
                    Rect rectCrop = createRect(extractPoints(parkingSlotDetail));
                    Mat croppedImage = new Mat(normalizedImage, rectCrop);
                    processSlotDetail(parkingSlotDetail, croppedImage, dataset);
                    lastRects.add(rectCrop);
                }

                classifyInstances(dataset, svmModel, lastPredictions);
            }

            drawPredictionsOnFrame(frame, lastRects, lastPredictions);
            videoWriter.write(frame);
        }
    }

    private void classifyInstances(Instances dataset, SMO svmModel, List<Double> predictions) {
        for (int i = 0; i < dataset.numInstances(); i++) {
            Instance instance = dataset.instance(i);
            try {
                predictions.add(svmModel.classifyInstance(instance));
            } catch (Exception e) {
                throw new RuntimeException("Failed to predict using the model.", e);
            }
        }
    }

    private void drawPredictionsOnFrame(Mat frame, List<Rect> lastRects, List<Double> lastPredictions) {
        for (int i = 0; i < lastRects.size(); i++) {
            Rect slotRect = lastRects.get(i);
            Scalar color = lastPredictions.get(i).equals(1.0) ? new Scalar(0, 0, 255) : new Scalar(0, 255, 0);
            Imgproc.rectangle(frame, slotRect.tl(), slotRect.br(), color, 2);
        }
    }

    private ParkingSlotDetail mapToParkingSlotDetail(ParkingSlotInfo slotInfo) {
        List<PointInfo> points = slotInfo.getCoordinatesInfos().stream()
                .map(this::mapToPointInfo)
                .toList();

        ParkingSlotDetail parkingSlotDetail = new ParkingSlotDetail();
        parkingSlotDetail.setPoints(points);
        return parkingSlotDetail;
    }

    private PointInfo mapToPointInfo(CoordinatesInfo coordinatesInfo) {
        return new PointInfo(
                coordinatesInfo.getId().intValue(),
                coordinatesInfo.getX(),
                coordinatesInfo.getY(),
                coordinatesInfo.getRealX(),
                coordinatesInfo.getRealY()
        );
    }
}
