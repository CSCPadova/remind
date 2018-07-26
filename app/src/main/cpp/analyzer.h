#include <opencv2/opencv.hpp>
#include <opencv2/imgcodecs.hpp>
#include <string>
#include <vector>

class Frame {

protected:
	cv::Mat frame_;
	std::vector<cv::Point> points_;
	std::vector<cv::Rect> rects_;

public:
	Frame();

	Frame(cv::Mat &frame);

	Frame(cv::Mat &frame, std::vector<cv::Point>& points,std::vector<cv::Rect>& rects);

	void getFrame(cv::Mat& frame);

	// ritorna tutti i punti
	void getPoints(std::vector<cv::Point>& points);

	void setPoints(std::vector<cv::Point>& points);

	// ritorna i punti prima del limite x
	void getPointsBefore(int x, std::vector<cv::Point>& points);

	// ritorna i punti dopo il limite x
	void getPointsAfter(int x, std::vector<cv::Point>& points,std::vector<cv::Rect>& rects);
};

class FrameAnalyzer {

protected:
	cv::Ptr<cv::BackgroundSubtractorKNN> background_subtractor_;
	Frame prev_frame_;

public:
	FrameAnalyzer();

	/*bool apply(cv::Mat* frame);*/

	bool apply(cv::Mat& frame);
};
