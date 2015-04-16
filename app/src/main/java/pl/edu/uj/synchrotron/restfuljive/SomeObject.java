package pl.edu.uj.synchrotron.restfuljive;

/**
 * Created by lukasz on 15.04.15.
 * This file is element of RESTful Jive application project.
 * You are free to use, copy and edit whole application or any of its components.
 * Application comes with no warranty. Altough author is trying to make it best, it may work or it may not work.
 */

/**
 * Class for storing name and tag of list element.
 */
class SomeObject {
	private String name;
	private String tag;
	private boolean isAlive;

	/**
	 * @param name Name of the object.
	 * @param tag  Tag of the object.
	 */
	public SomeObject(String name, String tag, boolean isAlive) {
		this.name = name;
		this.tag = tag;
		this.isAlive = isAlive;
	}

	/**
	 * Get name of the object.
	 *
	 * @return Name of the object.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get tag of the object.
	 *
	 * @return Tag of the object.
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * Check if device represented by object is alive
	 *
	 * @return if device is alive.
	 */
	public boolean getIsAlive() {
		return isAlive;
	}
}