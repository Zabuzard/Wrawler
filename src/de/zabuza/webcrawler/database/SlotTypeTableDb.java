package de.zabuza.webcrawler.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.zabuza.webcrawler.enums.SlotType;

/**
 * Utility class that links slotTypes and their ids.
 * 
 * @author Zabuza {@literal <zabuza.dev@gmail.com>}
 *
 */
public final class SlotTypeTableDb {
	/**
	 * Separator character for entries in the text representation.
	 */
	public static final String ENTRY_SEPARATOR = ";";
	/**
	 * Enclosing character for values in the text representation.
	 */
	private static final String ENCLOSER = "\"";
	/**
	 * Singleton instance of this class.
	 */
	private static SlotTypeTableDb instance = null;
	/**
	 * Separator character for values in the text representation.
	 */
	private static final String VALUE_SEPARATOR = ",";

	/**
	 * Gets the singleton instance of this class.
	 * 
	 * @return Singleton instance of this class
	 */
	public static SlotTypeTableDb getInstance() {
		if (instance == null) {
			instance = new SlotTypeTableDb();
		}
		return instance;
	}

	/**
	 * Dictionary for id to slot type access.
	 */
	private final Map<Integer, SlotType> idToSlotType = new HashMap<>();
	/**
	 * Dictionary for slot type to description access.
	 */
	private final Map<SlotType, String> slotTypeToDescription = new HashMap<>();
	/**
	 * Dictionary for slot type to id access.
	 */
	private final Map<SlotType, Integer> slotTypeToId = new HashMap<>();

	/**
	 * Creates a new slotTypeTableDb object.
	 */
	private SlotTypeTableDb() {
		// Generated by SlotTypeTableParseTool : slotTypeTable.csv
		this.idToSlotType.put(Integer.valueOf(1), SlotType.CO);
		this.slotTypeToDescription.put(SlotType.CO, "Commanding Officer");
		this.idToSlotType.put(Integer.valueOf(2), SlotType.XO);
		this.slotTypeToDescription.put(SlotType.XO, "Executive Officer");
		this.idToSlotType.put(Integer.valueOf(3), SlotType.MIO);
		this.slotTypeToDescription.put(SlotType.MIO, "Military Intelligence Officer");
		this.idToSlotType.put(Integer.valueOf(4), SlotType.COL);
		this.slotTypeToDescription.put(SlotType.COL, "Chief of Logistics");
		this.idToSlotType.put(Integer.valueOf(5), SlotType.JTAC);
		this.slotTypeToDescription.put(SlotType.JTAC, "Joint Terminal Attack Controller");
		this.idToSlotType.put(Integer.valueOf(6), SlotType.PL);
		this.slotTypeToDescription.put(SlotType.PL, "Platoon Leader");
		this.idToSlotType.put(Integer.valueOf(7), SlotType.PSG);
		this.slotTypeToDescription.put(SlotType.PSG, "Platoon Sergeant");
		this.idToSlotType.put(Integer.valueOf(8), SlotType.FO);
		this.slotTypeToDescription.put(SlotType.FO, "Forward Observer");
		this.idToSlotType.put(Integer.valueOf(9), SlotType.SL);
		this.slotTypeToDescription.put(SlotType.SL, "Squad Leader");
		this.idToSlotType.put(Integer.valueOf(10), SlotType.TL);
		this.slotTypeToDescription.put(SlotType.TL, "Team Leader");
		this.idToSlotType.put(Integer.valueOf(11), SlotType.FTL);
		this.slotTypeToDescription.put(SlotType.FTL, "Fireteam Leader");
		this.idToSlotType.put(Integer.valueOf(12), SlotType.AR);
		this.slotTypeToDescription.put(SlotType.AR, "Automatic Rifleman");
		this.idToSlotType.put(Integer.valueOf(13), SlotType.GRE);
		this.slotTypeToDescription.put(SlotType.GRE, "Grenadier");
		this.idToSlotType.put(Integer.valueOf(14), SlotType.RFL);
		this.slotTypeToDescription.put(SlotType.RFL, "Rifleman");
		this.idToSlotType.put(Integer.valueOf(15), SlotType.CMDC);
		this.slotTypeToDescription.put(SlotType.CMDC, "Combat Medic");
		this.idToSlotType.put(Integer.valueOf(16), SlotType.DM);
		this.slotTypeToDescription.put(SlotType.DM, "Designated Marksman");
		this.idToSlotType.put(Integer.valueOf(17), SlotType.ATR);
		this.slotTypeToDescription.put(SlotType.ATR, "Anti-Tank Rifleman");
		this.idToSlotType.put(Integer.valueOf(18), SlotType.AAR);
		this.slotTypeToDescription.put(SlotType.AAR, "Anti-Air Rifleman");
		this.idToSlotType.put(Integer.valueOf(19), SlotType.MG);
		this.slotTypeToDescription.put(SlotType.MG, "Machine Gunner");
		this.idToSlotType.put(Integer.valueOf(20), SlotType.AMG);
		this.slotTypeToDescription.put(SlotType.AMG, "Assistant Machine Gunner");
		this.idToSlotType.put(Integer.valueOf(21), SlotType.AT);
		this.slotTypeToDescription.put(SlotType.AT, "Anti-Tank Specialist");
		this.idToSlotType.put(Integer.valueOf(22), SlotType.AAT);
		this.slotTypeToDescription.put(SlotType.AAT, "Assistant Anti-Tank Specialist");
		this.idToSlotType.put(Integer.valueOf(23), SlotType.AA);
		this.slotTypeToDescription.put(SlotType.AA, "Anti-Air Specialist");
		this.idToSlotType.put(Integer.valueOf(24), SlotType.AAA);
		this.slotTypeToDescription.put(SlotType.AAA, "Assistant Anti-Air Specialist");
		this.idToSlotType.put(Integer.valueOf(25), SlotType.SNP);
		this.slotTypeToDescription.put(SlotType.SNP, "Sniper");
		this.idToSlotType.put(Integer.valueOf(26), SlotType.SPT);
		this.slotTypeToDescription.put(SlotType.SPT, "Spotter");
		this.idToSlotType.put(Integer.valueOf(27), SlotType.TPL);
		this.slotTypeToDescription.put(SlotType.TPL, "Tank Platoon Leader");
		this.idToSlotType.put(Integer.valueOf(28), SlotType.TPS);
		this.slotTypeToDescription.put(SlotType.TPS, "Tank Platoon Sergeant");
		this.idToSlotType.put(Integer.valueOf(29), SlotType.TC);
		this.slotTypeToDescription.put(SlotType.TC, "Tank Commander");
		this.idToSlotType.put(Integer.valueOf(30), SlotType.GNR);
		this.slotTypeToDescription.put(SlotType.GNR, "Gunner");
		this.idToSlotType.put(Integer.valueOf(31), SlotType.DRV);
		this.slotTypeToDescription.put(SlotType.DRV, "Driver");
		this.idToSlotType.put(Integer.valueOf(32), SlotType.LOG);
		this.slotTypeToDescription.put(SlotType.LOG, "Logistician");
		this.idToSlotType.put(Integer.valueOf(33), SlotType.MDC);
		this.slotTypeToDescription.put(SlotType.MDC, "Medic");
		this.idToSlotType.put(Integer.valueOf(34), SlotType.PIL);
		this.slotTypeToDescription.put(SlotType.PIL, "Pilot");
		this.idToSlotType.put(Integer.valueOf(35), SlotType.CPIL);
		this.slotTypeToDescription.put(SlotType.CPIL, "Co-Pilot");
		this.idToSlotType.put(Integer.valueOf(36), SlotType.WSO);
		this.slotTypeToDescription.put(SlotType.WSO, "Weapon Systems Officer");
		this.idToSlotType.put(Integer.valueOf(37), SlotType.WCO);
		this.slotTypeToDescription.put(SlotType.WCO, "Wing Commander");
		this.idToSlotType.put(Integer.valueOf(38), SlotType.ACSO);
		this.slotTypeToDescription.put(SlotType.ACSO, "Artillery Command Systems Operator");
		this.idToSlotType.put(Integer.valueOf(39), SlotType.SPEC);
		this.slotTypeToDescription.put(SlotType.SPEC, "Special Forces");
		this.idToSlotType.put(Integer.valueOf(40), SlotType.ZC_PLUS);
		this.slotTypeToDescription.put(SlotType.ZC_PLUS, "Zeus/CO+/etc.");
		this.idToSlotType.put(Integer.valueOf(41), SlotType.CE);
		this.slotTypeToDescription.put(SlotType.CE, "Combat Engineer");
		this.idToSlotType.put(Integer.valueOf(42), SlotType.OTHER);
		this.slotTypeToDescription.put(SlotType.OTHER, "Other");
		this.idToSlotType.put(Integer.valueOf(43), SlotType.RESERVE);
		this.slotTypeToDescription.put(SlotType.RESERVE, "Reserve");

		for (final Entry<Integer, SlotType> entry : this.idToSlotType.entrySet()) {
			this.slotTypeToId.put(entry.getValue(), entry.getKey());
		}
	}

	/**
	 * Returns the id of the given slot type.
	 * 
	 * @param type
	 *            Slot type to get id of
	 * @return Id of the slot type
	 */
	public Integer getId(final SlotType type) {
		final Integer id = this.slotTypeToId.get(type);
		if (id == null) {
			System.err.println("Database table does not know id of slot type: " + type);
		}
		return id;
	}

	/**
	 * Returns the slot type that is represented by the given id.
	 * 
	 * @param id
	 *            Id of the slot type
	 * @return Slot type that is represented by the given id
	 */
	public SlotType getSlotType(final int id) {
		final SlotType type = this.idToSlotType.get(Integer.valueOf(id));
		if (type == null) {
			System.err.println("Database table does not know slot type with id: " + id);
		}
		return type;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		for (final Entry<Integer, SlotType> entry : this.idToSlotType.entrySet()) {
			final int id = entry.getKey().intValue();
			final String description = this.slotTypeToDescription.get(entry.getValue());
			String slotTypeText = entry.getValue() + "";
			if (slotTypeText.equals("COL")) {
				slotTypeText = "CoL";
			} else if (slotTypeText.equals("ZC_PLUS")) {
				slotTypeText = "ZC+";
			} else if (slotTypeText.equals("OTHER")) {
				slotTypeText = "N/A";
			} else if (slotTypeText.equals("RESERVE")) {
				slotTypeText = "RE";
			}
			builder.append(ENCLOSER + id + ENCLOSER + VALUE_SEPARATOR);
			builder.append(ENCLOSER + slotTypeText + ENCLOSER + VALUE_SEPARATOR);
			builder.append(ENCLOSER + description + ENCLOSER + ENTRY_SEPARATOR);
		}
		builder.delete(builder.length() - ENTRY_SEPARATOR.length(), builder.length());
		return builder.toString();
	}
}
