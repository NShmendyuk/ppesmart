package ru.itmo.ppesmart;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import java.util.Objects;

@DataType()
public final class PPE {

    /**
     * Инвентарный номер СИЗа
     */
    @Property()
    private final String inventoryNumber;

    /**
     * Фамилия, имя, отчество сотрудника
     */
    @Property()
    private final String ownerName;

    /**
     * Табельный номер сотрудника
     */
    @Property()
    private final String ownerID;

    /**
     * Название средства индивидуальной защиты
     */
    @Property()
    private final String name;

    /**
     * Цена СИЗа
     */
    @Property()
    private final Float price;

    @Property()
    private final String status;

    /**
     * Дата поступления СИЗа в эксплуатацию
     */
    //TODO: timestamp
    @Property()
    private final String startUseDate;

    /**
     * Срок службы
     */
    @Property()
    private final Integer lifeTime;

    /**
     * Организация
     */
    @Property()
    private final String subsidiary;

    public String getOwnerName() {
        return ownerName;
    }

    public String getOwnerID() {
        return ownerID;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public Float getPrice() {
        return price;
    }

    public String getInventoryNumber() {
        return inventoryNumber;
    }

    public String getStartUseDate() {
        return startUseDate;
    }

    public Integer getLifeTime() {
        return lifeTime;
    }

    public String getSubsidiary() {
        return subsidiary;
    }

    public PPE(@JsonProperty("ownerName") final String ownerName,
               @JsonProperty("ownerID") final String ownerID, @JsonProperty("name") final String name, @JsonProperty("status") final String status,
               @JsonProperty("price") final Float price, @JsonProperty("inventoryNumber") final String inventoryNumber,
               @JsonProperty("startUserDate") final String startUseDate, @JsonProperty("lifeTime") final Integer lifeTime,
               @JsonProperty("subsidiary") final String subsidiary) {
        this.ownerName = ownerName;
        this.ownerID = ownerID;
        this.name = name;
        this.status = status;
        this.price = price;
        this.inventoryNumber = inventoryNumber;
        this.startUseDate = startUseDate;
        this.lifeTime = lifeTime;
        this.subsidiary = subsidiary;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        PPE other = (PPE) obj;

        return Objects.deepEquals(
                new String[] {getOwnerName(), getOwnerID(), getName(), getStartUseDate(), getInventoryNumber()},
                new String[] {other.getOwnerName(), other.getOwnerID(), other.getName(), other.getStartUseDate(), other.getInventoryNumber()})
                &&
                Objects.deepEquals(
                new int[] {getLifeTime()},
                new int[] {other.getLifeTime()});
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwnerName(), getOwnerID(),
                getName(), getStatus(), getPrice(), getInventoryNumber(),
                getStartUseDate(), getLifeTime(), getSubsidiary());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [" +
                ", ownerName = " + ownerName + ", ownerID = " + ownerID + ", subsidiary = " + subsidiary +
                ", PPE name = " + name + ", price = " + price + ", inventory number = " + inventoryNumber +
                ", status = " + status + ", date of entry into service = " + startUseDate + ", lifeTime = "
                + lifeTime + " month" +"]";
    }
}
