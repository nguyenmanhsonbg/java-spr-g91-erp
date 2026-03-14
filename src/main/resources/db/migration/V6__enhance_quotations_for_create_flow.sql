ALTER TABLE quotations
ADD COLUMN note VARCHAR(1000) NULL,
ADD COLUMN delivery_requirement VARCHAR(1000) NULL;

ALTER TABLE quotations
ADD CONSTRAINT uk_quotation_number UNIQUE (quotation_number);

ALTER TABLE quotations
ADD CONSTRAINT fk_quotation_project
FOREIGN KEY (project_id) REFERENCES projects(id);
