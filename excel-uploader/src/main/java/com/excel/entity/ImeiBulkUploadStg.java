package com.excel.entity;

import java.util.Date;
import jakarta.persistence.*;

@Entity
@Table(name = "IMEI_BULK_UPLOAD_STG")
public class ImeiBulkUploadStg {

    /* ================= PRIMARY KEY ================= */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SNO")
    private Long sno;

    /* ================= BUSINESS COLUMNS ================= */

    @Column(name = "STORERKEY", length = 50)
    private String storerKey;

    @Column(name = "ORDERKEY")
    private String orderKey;

    @Column(name = "WHSE", length = 20)
    private String whse;

    @Column(name = "ASN", length = 50)
    private String asn;

    @Column(name = "ASNLINE", length = 20)
    private String asnLine;

    @Column(name = "SKU", length = 50)
    private String sku;

    @Column(name = "ID", length = 50)
    private String itemId;

    @Column(name = "SERIAL", length = 100)
    private String serial;

    @Column(name = "PARENTSERIAL", length = 100)
    private String parentSerial;

    @Column(name = "FILENAME", length = 255)
    private String filename;

    @Column(name = "VERSION")
    private Integer version;

    @Column(name = "TYPE", length = 1)
    private String type; // I / O

    /* ================= AUDIT COLUMNS ================= */

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ADDDATE")
    private Date addDate;

    @Column(name = "ADDWHO", length = 50)
    private String addWho;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "EDITDATE")
    private Date editDate;

    @Column(name = "EDITWHO", length = 50)
    private String editWho;

    @Column(name = "PROCESS_FLAG")
    private Integer processFlag;

    /* ================= GETTERS / SETTERS ================= */

    public Long getSno() {
        return sno;
    }

    public String getStorerKey() {
        return storerKey;
    }

    public void setStorerKey(String storerKey) {
        this.storerKey = storerKey;
    }

    public String getOrderKey() {
        return orderKey;
    }

    public void setOrderKey(String orderKey) {
        this.orderKey = orderKey;
    }

    public String getWhse() {
        return whse;
    }

    public void setWhse(String whse) {
        this.whse = whse;
    }

    public String getAsn() {
        return asn;
    }

    public void setAsn(String asn) {
        this.asn = asn;
    }

    public String getAsnLine() {
        return asnLine;
    }

    public void setAsnLine(String asnLine) {
        this.asnLine = asnLine;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getParentSerial() {
        return parentSerial;
    }

    public void setParentSerial(String parentSerial) {
        this.parentSerial = parentSerial;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getAddDate() {
        return addDate;
    }

    public void setAddDate(Date addDate) {
        this.addDate = addDate;
    }

    public String getAddWho() {
        return addWho;
    }

    public void setAddWho(String addWho) {
        this.addWho = addWho;
    }

    public Date getEditDate() {
        return editDate;
    }

    public void setEditDate(Date editDate) {
        this.editDate = editDate;
    }

    public String getEditWho() {
        return editWho;
    }

    public void setEditWho(String editWho) {
        this.editWho = editWho;
    }

    public Integer getProcessFlag() {
        return processFlag;
    }

    public void setProcessFlag(Integer processFlag) {
        this.processFlag = processFlag;
    }
}
