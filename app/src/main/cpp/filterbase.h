#ifndef FILTERBASE_H
#define FILTERBASE_H

#include <thread>
#include <jni.h>

class FilterBase
{
	public:
		FilterBase();
		virtual ~FilterBase();

		virtual void run();				// fa partire il thread di esecuzione
		virtual void scheduleStop();		// Indica al blocco che dovrebbe fermarsi appena possibile
		virtual void join();				// Attende che il thread di esecuzione si fermi (deve essere stato già chiamato scheduleStop())

	protected:
		virtual void execute();			// Funzione eseguita dal thread: esegue loop() in ciclo
		virtual bool loop() = 0;			// da implementare col codice del filtro; deve tornare false se il ciclo si ha da interrompere, altrimenti true

	private:
		std::thread execThread;
		volatile bool mustStop;
};

#endif // FILTERBASE_H
