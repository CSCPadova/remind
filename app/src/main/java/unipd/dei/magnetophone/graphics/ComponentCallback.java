package unipd.dei.magnetophone.graphics;

/**
 * Interfaccia che espone una callback richiamata quando
 * il componente cambia il suo stato.
 * Ad esempio: un pulsante premuto, una manopola girata, ecc
 *
 * @author daniele
 */
public interface ComponentCallback {

    /**
     * Evento generato quando cambia lo stato del componente
     *
     * @param obj Componente interessato
     */
    public void stateChanged(UIComponent obj);
}
