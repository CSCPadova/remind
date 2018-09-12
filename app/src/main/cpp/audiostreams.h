#ifndef AUDIOSTREAMS_H
#define AUDIOSTREAMS_H

/**
 * AudioStreams è una libreria che permette di creare flussi di dati audio
 * da sorgenti (sources) e destinazioni (sinks).
 *
 *
 * Nota implementativa:
 * L'OutputStream contiene la CircularQueue usata per il trasferimento dati.
 * InputStream usa la queue attraverso il riferimento all'OutputStream collegato.
 */

#include <circularqueue.h>

#include <mutex>
#include <condition_variable>
#include <array>

namespace audio
{
	// Il buffer per passare l'audio da un blocco all'altro
	constexpr int AudioBufferSize = 1024;//1024
	using AudioBuffer = std::array<float, AudioBufferSize>;

	enum class Status {
		OK,
		TIMEOUT,
		ERROR
	};

	class OutputStream;	// forward declaration
	/**
	 * InputStream: stream che prende dati in ingresso per un blocco da un OutputStream
	 */
	class InputStream
	{
		friend class OutputStream;
		friend void connect(InputStream &i, OutputStream &o);

		public:
			InputStream();
			~InputStream();

			// ritorna true se ci sono dati in attesa di esser ricevuti
			bool hasData();

			// Copia un buffer di dati dalla coda, torna false in caso di errore altrimenti true
			Status pullData(AudioBuffer &destBuffer);

			// Attende finchè non si presentano elementi nello stream.
			Status waitIfEmpty();

			void flush();
		private:
			OutputStream *connectedOutput = nullptr;
			bool attachOutput(OutputStream &out);
			void removeOutput(bool withoutNotify=false);
	};


	/**
	 * OutputStream: stream che fornisce dati in uscita da un blocco verso un altro InputStream
	 */
	class OutputStream
	{
		friend class InputStream;
		friend void connect(InputStream &i, OutputStream &o);

		public:
			OutputStream();
			~OutputStream();

			// Ritorna true se la coda è piena
			bool isFull();

			// Inserisce una copia del buffer nello stream
			Status pushData(const AudioBuffer &srcBuffer);

			// Attende finchè non si liberano posizioni nello stream.
			Status waitIfFull();

			void flush();

		private:
			InputStream *connectedInput = nullptr;
			bool attachInput(InputStream &in);
			void removeInput(bool withoutNotify=false);

			CircularQueue<audio::AudioBuffer> bufferQueue;
	};


	/**
	 * connect collega un InputStream a un OutputStream in modo che possano passarsi i dati
	 */
	void connect(InputStream &i, OutputStream &o);
	inline void connect(OutputStream &o, InputStream &i) { connect(i, o); }


} // namespace

extern template class CircularQueue<audio::AudioBuffer>;	// Il template è esplicitamente definito in audiostreams.cpp

#endif // AUDIOSTREAMS_H
