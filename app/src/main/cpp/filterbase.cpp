#include "filterbase.h"

FilterBase::FilterBase() {
	mustStop = false;
}

FilterBase::~FilterBase() {
	scheduleStop();
	join();
}

// fa partire il thread di esecuzione
void FilterBase::run()
{
	mustStop = false;
	execThread = std::thread(&FilterBase::execute, this);
}

// Indica al blocco che dovrebbe fermarsi appena possibile
void FilterBase::scheduleStop()
{
	mustStop = true;
}

// Attende che il thread di esecuzione si fermi (deve essere stato già chiamato scheduleStop())
void FilterBase::join()
{
	if(execThread.joinable())
		execThread.join();
}


void FilterBase::execute()
{
	while(!mustStop && loop()) /**/;
}
