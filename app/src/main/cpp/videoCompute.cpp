/*
 * Metodi nativi per il decoding dei frame video tramite la libreria ffmpeg
 * @author Nicola Castaman
 * @author Mirco Maniero
 * @author Laura Nao
 * */

/*Headers Android*/
#include <jni.h>
#include <android/log.h>

/*Headers standard library*/
#include <time.h>
#include <math.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include <unistd.h>
#include <assert.h>

#include <opencv2/core.hpp>

/*Headers ffmpeg*/
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>

}

#include "analyzer.h"

/*Define per i Log Android*/
#define LOG_TAG "VideoCompute"
#define LOG_LEVEL 10
#define LOGI(level, ...) if (level <= LOG_LEVEL) {__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);}
#define LOGE(level, ...) if (level <= LOG_LEVEL) {__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);}

extern "C" {
JNIEXPORT jint JNICALL Java_unipd_dei_whatsnew_videoprocessing_VideoProcessingService_videoCompute(JNIEnv *pEnv, jobject pObj, jstring fileName, jstring folderPath) {

	// Inizializzazione a NULL di tutte le variabili per evitare Segmentation faults
	AVFormatContext *pFormatCtx = NULL;
	int i, videoStream;
	AVCodecContext *pCodecCtxOrig = NULL;
	AVCodecContext *pCodecCtx = NULL;
	AVCodec *pCodec = NULL;
	AVFrame *pFrame = NULL;
	AVFrame *pFrameRGB = NULL;
	AVPacket packet;
	int frameFinished;
	int numBytes;
	uint8_t *buffer = NULL;
	struct SwsContext *sws_ctx = NULL;

	char *pFileName;//Nome del video
	char *pFolderPath;//Cartella in cui salvare i frame

	pFileName = (char *) pEnv->GetStringUTFChars(fileName, NULL);

	pFolderPath = (char *) pEnv->GetStringUTFChars(folderPath, NULL);

	// Registra tutti i Format e i Codec
	av_register_all();

	// Aperture file video
	if(avformat_open_input(&pFormatCtx, pFileName, NULL, NULL)!=0) {
		LOGE(1, "Couldn't open file");
		return -1; // Impossibile aprire il file
	}

	// Ottengo le informazioni sullo stream
	if(avformat_find_stream_info(pFormatCtx, NULL)<0) {
		LOGE(1, "Couldn't find stream information");
		return -1; // Impossibile ottenere le informazioni sullo stream
	}

	// Trovo il primo stream video
	videoStream=-1;
	for(i=0; i<pFormatCtx->nb_streams; i++)
	if(pFormatCtx->streams[i]->codec->codec_type==AVMEDIA_TYPE_VIDEO) {
		videoStream=i;
		break;
	}
	if(videoStream==-1) {
		LOGE(1, "Didn't find a video stream");
		return -1; // Nessuno stream trovato
	}

	// Ottengo un puntatore al codec context per lo stream video
	pCodecCtxOrig=pFormatCtx->streams[videoStream]->codec;
	// Trovo il decoder per lo stream video
	pCodec=avcodec_find_decoder(pCodecCtxOrig->codec_id);
	if(pCodec==NULL) {
		LOGE(1, "Unsupported codec!");
		return -1; // Codec non trovato
	}
	// Copio il contesto
	pCodecCtx = avcodec_alloc_context3(pCodec);
	if(avcodec_copy_context(pCodecCtx, pCodecCtxOrig) != 0) {
		LOGE(1, "Couldn't copy codec context");
		return -1; // Errore nella copia del contesto
	}

	// Apro codec
	if(avcodec_open2(pCodecCtx, pCodec, NULL)<0) {
		LOGE(1, "Could not open codec");
		return -1; // Impossibile aprire il codec
	}

	// Alloca frame video
	pFrame=avcodec_alloc_frame();

	// Allocata un AVFrame
	pFrameRGB=avcodec_alloc_frame();
	if(pFrameRGB==NULL)
	return -1;

	// Determino le dimensioni del buffer e lo alloco
	numBytes=avpicture_get_size(PIX_FMT_RGB24, pCodecCtx->width,
			pCodecCtx->height);
	buffer=(uint8_t *)av_malloc(numBytes*sizeof(uint8_t));

	avpicture_fill((AVPicture *)pFrameRGB, buffer, PIX_FMT_RGB24,
			pCodecCtx->width, pCodecCtx->height);

	// Inizializzo SWS context per il software scaling
	sws_ctx = sws_getContext(pCodecCtx->width,
			pCodecCtx->height,
			pCodecCtx->pix_fmt,
			pCodecCtx->width,
			pCodecCtx->height,
			PIX_FMT_RGB24,
			SWS_BILINEAR,
			NULL,
			NULL,
			NULL
	);

	// Elabora i frames e salva i risultati in memoria
	FrameAnalyzer fa;
	int num_frame = 0;
	while(av_read_frame(pFormatCtx, &packet)>=0) {
		// Se è un pacchetto dello stream video
		if(packet.stream_index==videoStream) {
			// Decodifica il frame video
			avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, &packet);

			// Se ho ottenuto un frame video
			if(frameFinished) {
				// Converto l'immagine in RGB
				sws_scale(sws_ctx, (uint8_t const * const *)pFrame->data,
						pFrame->linesize, 0, pCodecCtx->height,
						pFrameRGB->data, pFrameRGB->linesize);

				// Elaboro il frame
				cv::Mat frame(pFrame->height, pFrame->width, CV_8UC3,
						pFrameRGB->data[0], pFrameRGB->linesize[0]);


				if(fa.apply(frame)) {
					LOGI(1,"Frame found");
					std::stringstream ss;
					double ts;
					ts = av_frame_get_best_effort_timestamp(pFrame);
					ts = av_rescale_q ( ts,  pFormatCtx->streams[packet.stream_index]->time_base,AV_TIME_BASE_Q)/1000;
					ss << pFolderPath << "/" << num_frame << "_" << ts << ".jpg";
					std::string frameName = ss.str();
					cv::cvtColor(frame,frame,CV_BGR2RGB);
					try {
						cv::imwrite(frameName, frame);
					} catch (int e) {
						return -2;
					}

				}
			}
			LOGE(1,"Frame %d",num_frame);
			num_frame++;
		}

		// Libero il pacchetto che era stato allocato da av_read_frame
		av_free_packet(&packet);
	}

	// Libero l'immagine RGB
	av_free(buffer);
	av_frame_free(&pFrameRGB);

	av_frame_free(&pFrame);

	// Chiudo i codec
	avcodec_close(pCodecCtx);
	avcodec_close(pCodecCtxOrig);

	// Chiudo il file video
	pFormatCtx->flush_packets;
	pFormatCtx=NULL;

	return 0;
}
}
