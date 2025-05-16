/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */

package transport.core;

/**
 *
 * @author LATITUDE
 */
public enum ReclamationStatus {
    EN_COURS("En cours"),
    TRAITE("Traité"),
    REFUSE("Refusé"),
    ANNULE("Annulé");

    private String status;

    ReclamationStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return status;
    }

}