package com.psddev.cms.tool;

import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.Set;

import com.psddev.dari.db.Record;
import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.ObjectUtils;

/** Small */
public class Widget extends Plugin {

    private String iconName;
    private Set<Position> positions;
    private Set<Widget> updateDependencies;

    /** Returns the icon name. */
    public String getIconName() {
        return iconName;
    }

    /** Sets the icon name. */
    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    /** Returns the positions. */
    public Set<Position> getPositions() {
        if (positions == null) {
            positions = new LinkedHashSet<Position>();
        }
        return positions;
    }

    /** Sets the positions. */
    public void setPositions(Set<Position> positions) {
        this.positions = positions;
    }

    /** Returns the update dependencies. */
    public Set<Widget> getUpdateDependencies() {
        if (updateDependencies == null) {
            updateDependencies = new LinkedHashSet<Widget>();
        }
        return updateDependencies;
    }

    /** Sets the update dependencies. */
    public void setUpdateDependencies(Set<Widget> dependencies) {
        this.updateDependencies = dependencies;
    }

    /**
     * Returns the unique ID that represents this widget for use in
     * permissions.
     */
    public String getPermissionId() {
        return "widget/" + getInternalName();
    }

    /**
     * Adds a position with the given {@code name}, {@code column},
     * and {@code row}.
     */
    public void addPosition(String name, double column, double row) {
        Position position = new Position();
        position.setName(name);
        position.setColumn(column);
        position.setRow(row);
        getPositions().add(position);
    }

    public boolean shouldDisplayInNonPublishable() {
        return false;
    }

    /**
     * Creates the display HTML of this widget for the given {@code object}.
     */
    @SuppressWarnings("resource")
    public String createDisplayHtml(ToolPageContext page, Object object) throws Exception {
        try {
            String displayHtml = display(page, object);

            if (ObjectUtils.isBlank(displayHtml)) {
                return null;

            } else {
                StringWriter sw = new StringWriter();
                HtmlWriter writer = new HtmlWriter(sw);

                writer.writeStart("div", "class", "widget widget-" + getInternalName());
                    writer.writeStart("h1");
                        writer.writeHtml(page.getObjectLabel(this));
                    writer.writeEnd();
                    writer.write(displayHtml);
                writer.writeEnd();

                return sw.toString();
            }

        } catch (UnsupportedOperationException error) {
            throw error;
        }
    }

    /** Updates the given {@code object}. */
    public void update(ToolPageContext page, Object object) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Embedded
    public static class Position extends Record {

        @Required
        private String name;

        @Required
        private double column;

        @Required
        private double row;

        /** Returns the name. */
        public String getName() {
            return name;
        }

        /** Sets the name. */
        public void setName(String name) {
            this.name = name;
        }

        /** Returns the column. */
        public double getColumn() {
            return column;
        }

        /** Sets the column. */
        public void setColumn(double column) {
            this.column = column;
        }

        /** Returns the row. */
        public double getRow() {
            return row;
        }

        /** Sets the row. */
        public void setRow(double row) {
            this.row = row;
        }
    }

    // --- Deprecated ---

    /** @deprecated Use {@link #createDisplayHtml} instead. */
    @Deprecated
    public String display(ToolPageContext page, Object object) throws Exception {
        throw new UnsupportedOperationException();
    }
}
