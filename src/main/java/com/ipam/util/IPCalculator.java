package com.ipam.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utilitaire pour les calculs réseau IP
 */
public class IPCalculator {
    
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    private static final Pattern MAC_PATTERN = Pattern.compile(
        "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"
    );

    /**
     * Valide une adresse IP
     */
    public static boolean isValidIP(String ip) {
        return ip != null && IP_PATTERN.matcher(ip).matches();
    }

    /**
     * Valide une adresse MAC
     */
    public static boolean isValidMAC(String mac) {
        return mac != null && MAC_PATTERN.matcher(mac).matches();
    }

    /**
     * Convertit une adresse IP en entier long
     */
    public static long ipToLong(String ip) {
        if (!isValidIP(ip)) {
            throw new IllegalArgumentException("Adresse IP invalide: " + ip);
        }
        
        String[] octets = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | Integer.parseInt(octets[i]);
        }
        return result;
    }

    /**
     * Convertit un entier long en adresse IP
     */
    public static String longToIp(long ip) {
        return String.format("%d.%d.%d.%d",
            (ip >> 24) & 0xFF,
            (ip >> 16) & 0xFF,
            (ip >> 8) & 0xFF,
            ip & 0xFF
        );
    }

    /**
     * Convertit CIDR en masque de sous-réseau
     */
    public static String cidrToSubnetMask(int cidr) {
        if (cidr < 0 || cidr > 32) {
            throw new IllegalArgumentException("CIDR doit être entre 0 et 32");
        }
        
        long mask = ((1L << cidr) - 1) << (32 - cidr);
        return longToIp(mask);
    }

    /**
     * Convertit masque de sous-réseau en CIDR
     */
    public static int subnetMaskToCidr(String mask) {
        if (!isValidIP(mask)) {
            throw new IllegalArgumentException("Masque de sous-réseau invalide");
        }
        
        long maskLong = ipToLong(mask);
        int cidr = 0;
        while ((maskLong & (1L << (31 - cidr))) != 0 && cidr < 32) {
            cidr++;
        }
        return cidr;
    }

    /**
     * Calcule l'adresse réseau à partir d'une IP et d'un CIDR
     */
    public static String getNetworkAddress(String ip, int cidr) {
        long ipLong = ipToLong(ip);
        long maskLong = ipToLong(cidrToSubnetMask(cidr));
        long networkLong = ipLong & maskLong;
        return longToIp(networkLong);
    }

    /**
     * Calcule l'adresse de broadcast
     */
    public static String getBroadcastAddress(String networkAddress, int cidr) {
        long networkLong = ipToLong(networkAddress);
        long maskLong = ipToLong(cidrToSubnetMask(cidr));
        long broadcastLong = networkLong | (~maskLong & 0xFFFFFFFFL);
        return longToIp(broadcastLong);
    }

    /**
     * Calcule la première adresse IP utilisable
     */
    public static String getFirstUsableIp(String networkAddress, int cidr) {
        if (cidr == 31 || cidr == 32) {
            return networkAddress; // Cas particuliers
        }
        long networkLong = ipToLong(networkAddress);
        return longToIp(networkLong + 1);
    }

    /**
     * Calcule la dernière adresse IP utilisable
     */
    public static String getLastUsableIp(String networkAddress, int cidr) {
        if (cidr == 32) {
            return networkAddress; // /32 = une seule IP
        }
        String broadcast = getBroadcastAddress(networkAddress, cidr);
        long broadcastLong = ipToLong(broadcast);
        
        if (cidr == 31) {
            return broadcast; // /31 utilise les deux IPs
        }
        return longToIp(broadcastLong - 1);
    }

    /**
     * Calcule le nombre total d'hôtes
     */
    public static int getTotalHosts(int cidr) {
        if (cidr == 32) return 1;
        if (cidr == 31) return 2;
        return (int) Math.pow(2, 32 - cidr) - 2; // -2 pour réseau et broadcast
    }

    /**
     * Vérifie si une IP appartient à un sous-réseau
     */
    public static boolean isIpInSubnet(String ip, String networkAddress, int cidr) {
        long ipLong = ipToLong(ip);
        long networkLong = ipToLong(networkAddress);
        long maskLong = ipToLong(cidrToSubnetMask(cidr));
        
        return (ipLong & maskLong) == (networkLong & maskLong);
    }

    /**
     * Génère toutes les adresses IP utilisables d'un sous-réseau
     */
    public static List<String> getAllUsableIps(String networkAddress, int cidr) {
        List<String> ips = new ArrayList<>();
        
        String firstIp = getFirstUsableIp(networkAddress, cidr);
        String lastIp = getLastUsableIp(networkAddress, cidr);
        
        long start = ipToLong(firstIp);
        long end = ipToLong(lastIp);
        
        // Limite de sécurité pour éviter les gros sous-réseaux
        int maxIps = 65536; // /16 au maximum
        int count = 0;
        
        for (long i = start; i <= end && count < maxIps; i++) {
            ips.add(longToIp(i));
            count++;
        }
        
        return ips;
    }

    /**
     * Vérifie si deux sous-réseaux se chevauchent
     */
    public static boolean subnetsOverlap(String network1, int cidr1, String network2, int cidr2) {
        long net1 = ipToLong(network1);
        long net2 = ipToLong(network2);
        long mask1 = ipToLong(cidrToSubnetMask(cidr1));
        long mask2 = ipToLong(cidrToSubnetMask(cidr2));
        
        long broadcast1 = net1 | (~mask1 & 0xFFFFFFFFL);
        long broadcast2 = net2 | (~mask2 & 0xFFFFFFFFL);
        
        return !(broadcast1 < net2 || broadcast2 < net1);
    }

    /**
     * Formate une adresse MAC
     */
    public static String formatMAC(String mac) {
        if (mac == null) return null;
        
        String clean = mac.replaceAll("[^0-9A-Fa-f]", "");
        if (clean.length() != 12) return mac;
        
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < 12; i += 2) {
            if (i > 0) formatted.append(":");
            formatted.append(clean.substring(i, i + 2).toUpperCase());
        }
        return formatted.toString();
    }

    /**
     * Calcule le nombre de sous-réseaux possibles
     */
    public static int getNumberOfSubnets(int originalCidr, int newCidr) {
        if (newCidr < originalCidr) {
            throw new IllegalArgumentException("Le nouveau CIDR doit être >= au CIDR original");
        }
        return (int) Math.pow(2, newCidr - originalCidr);
    }
}
