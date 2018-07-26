/*
 * Classe che elabora i frame utilizzando le librerie openCV
 * @author Nicola Castaman
 * @author Mirco Maniero
 * @author Laura Nao
 * */

#include "analyzer.h"

Frame::Frame() {
}

Frame::Frame(cv::Mat &frame) {
	frame_ = frame;
}

Frame::Frame(cv::Mat &frame, std::vector<cv::Point>& points,std::vector<cv::Rect>& rects) {
	frame_ = frame;
	points_ = points;
	rects_=rects;
}

void
Frame::getFrame(cv::Mat& frame) {
	frame = frame_;
}

// ritorna tutti i punti
void
Frame::getPoints(std::vector<cv::Point>& points) {
	points = points_;
}

void
Frame::setPoints(std::vector<cv::Point>& points){
	points_ = points;
}

// ritorna i punti prima del limite x
void
Frame::getPointsBefore(int x, std::vector<cv::Point>& points) {
	for(int i = 0; i < points_.size(); i++){
		if(points_[i].x < x){
			points.push_back(points_[i]);
		}
	}
}

// ritorna i punti dopo il limite x
void
Frame::getPointsAfter(int x, std::vector<cv::Point>& points,std::vector<cv::Rect>& rects) {
	for(int i = 0; i < points_.size(); i++){
		if(points_[i].x >= x){
			points.push_back(points_[i]);
			rects.push_back(rects_[i]);

		}
	}
}


FrameAnalyzer::FrameAnalyzer(){
	background_subtractor_ = cv::createBackgroundSubtractorKNN(5, 500.0, false);
}


bool
FrameAnalyzer::apply(cv::Mat& frame){

	if(frame.empty())
	{
		return (false);
	}

	/// Ottengo il foreground
	cv::Mat foreground;
	background_subtractor_->apply(frame, foreground);

	/// Elaboro il foreground estratto
	cv::Mat element = getStructuringElement(cv::MORPH_ELLIPSE, cv::Size(9, 9));

	//closing
	cv::morphologyEx(foreground, foreground, cv::MORPH_CLOSE, element);

	//opening
	cv::morphologyEx(foreground, foreground, cv::MORPH_OPEN, element);

	std::vector<std::vector<cv::Point> > contours;
	cv::findContours(foreground, contours, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE);

	std::vector<std::vector<cv::Point> > contours_poly(contours.size());

	std::vector<cv::Rect> rects;

	std::vector<cv::Point> points;
	for(int i = 0; i < contours.size(); i++) {
		/**
		Approssimazione del contorno individuato con un quadrato; il primo parametro è la poligonale
		da approssimare, il secondo è il risultato, il terzo è la precisione dell'approssimazione,
		il quarto indica se la poligonale dev'essere chiusa o meno.
		*/
		approxPolyDP( cv::Mat(contours[i]), contours_poly[i], 5, true );
		//Calcola il rettangolo che approssima i contorni.
		cv::Rect rect = boundingRect( cv::Mat(contours_poly[i]) );
		//vincoli
		int MIN_HEIGHT = frame.rows/12;
		int MAX_HEIGHT = frame.rows/3;
		int MAX_WIDTH = frame.cols;
		if(rect.height > MIN_HEIGHT && rect.height < MAX_HEIGHT && rect.width < MAX_WIDTH){
			points.push_back(cv::Point(rect.x+rect.width, rect.y+rect.height/2));
			rects.push_back(rect);
		}
	}

	Frame curr_frame (frame, points,rects);

	//estraggo nuovi punti dopo limite
	std::vector<cv::Point> points_after_x;
	std::vector<cv::Rect> correspondent_rects;
	curr_frame.getPointsAfter(frame.cols*2/3, points_after_x,correspondent_rects);

	//estraggo vecchi punti prima limite
	std::vector<cv::Point> points_before_x;
	prev_frame_.getPointsBefore(frame.cols*2/3, points_before_x);

	prev_frame_ = curr_frame;

	//per ogni nuovo punto oltre il limite cerco se c'è un corrispondente
	//punto prima del limite
	for(int i = 0; i < points_after_x.size(); i++){
		for(int j = 0; j < points_before_x.size(); j++){
			if(points_after_x[i].y > points_before_x[j].y-10 && points_after_x[i].y < points_before_x[j].y+10 &&
				points_after_x[i].x > points_before_x[j].x+0 && points_after_x[i].x < points_before_x[j].x+frame.cols/3 ){
				cv::rectangle(frame,correspondent_rects[i],cv::Scalar(0,255,0),2);
					return (true);
				}
			}
		}
		return (false);
	}
